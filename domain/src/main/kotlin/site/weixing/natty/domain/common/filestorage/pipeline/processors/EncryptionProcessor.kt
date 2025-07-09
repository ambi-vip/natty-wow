package site.weixing.natty.domain.common.filestorage.pipeline.processors

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessorStatistics
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * 加密流处理器
 * 对文件内容进行流式加密，支持AES-256-GCM算法
 */
class EncryptionProcessor(
    private val configuration: EncryptionConfiguration = EncryptionConfiguration()
) : StreamProcessor {
    
    override val name: String = "EncryptionProcessor"
    override val priority: Int = 40 // 较低优先级，在压缩之后加密
    
    private var processedBytes: Long = 0
    private var startTime: Long = 0
    private var encryptionKey: SecretKey? = null
    private var iv: ByteArray? = null
    
    override fun isApplicable(context: ProcessingContext): Boolean {
        return context.processingOptions.enableEncryption && 
               shouldEncryptFile(context)
    }
    
    override fun initialize(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            processedBytes = 0
            startTime = System.currentTimeMillis()
            
            // 生成或获取加密密钥
            encryptionKey = generateOrGetEncryptionKey(context)
            
            // 生成初始化向量（IV）
            iv = generateIV()
            
            if (configuration.logActivity) {
                println("加密处理器初始化：${context.fileName}，算法：${configuration.algorithm}")
            }
        }
    }
    
    override fun process(input: Flux<ByteBuffer>, context: ProcessingContext): Flux<ByteBuffer> {
        return input
            .collectList() // 收集所有数据进行加密
            .flatMapMany { buffers ->
                // 合并所有buffer
                val totalSize = buffers.sumOf { it.remaining() }
                val inputBytes = ByteArray(totalSize)
                var offset = 0
                
                buffers.forEach { buffer ->
                    val remaining = buffer.remaining()
                    buffer.get(inputBytes, offset, remaining)
                    offset += remaining
                }
                
                processedBytes = inputBytes.size.toLong()
                
                // 执行加密
                val encryptedData = encryptBytes(inputBytes, context)
                
                // 返回加密后的数据（包含IV前缀）
                Flux.just(ByteBuffer.wrap(encryptedData))
            }
            .doOnComplete {
                // 记录加密信息到上下文
                context.addMetadata("encryptionAlgorithm", configuration.algorithm)
                context.addMetadata("encryptionKeyId", getKeyId(encryptionKey))
                context.addMetadata("ivBase64", Base64.getEncoder().encodeToString(iv ?: byteArrayOf()))
                context.addMetadata("encryptedSize", processedBytes)
                
                if (configuration.logActivity) {
                    println("加密完成：${context.fileName}，大小：${formatBytes(processedBytes)}")
                }
            }
    }
    
    override fun cleanup(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            // 清理加密器资源
            this.encryptionKey = null
            this.iv = null
            this.processedBytes = 0
        }
    }
    
    override fun getStatistics(): ProcessorStatistics {
        val endTime = System.currentTimeMillis()
        val processingTime = if (startTime > 0) endTime - startTime else 0
        
        return ProcessorStatistics(
            processorName = name,
            processedFiles = if (processedBytes > 0) 1 else 0,
            totalProcessingTime = processingTime,
            averageProcessingTime = processingTime.toDouble(),
            errorCount = 0,
            bytesProcessed = processedBytes,
            lastProcessingTime = processingTime
        )
    }
    
    /**
     * 判断文件是否需要加密
     */
    private fun shouldEncryptFile(context: ProcessingContext): Boolean {
        // 检查文件类型，某些类型可能不需要加密
        val publicContentTypes = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp"
        )
        
        // 从metadata中获取isPublic属性
        val isPublic = context.getMetadata<Boolean>("isPublic") ?: false
        
        // 公开文件且为常见图片格式，可能不需要加密
        if (isPublic && publicContentTypes.contains(context.contentType)) {
            return false
        }
        
        // 从metadata中获取tags
        val tags = context.getMetadata<List<String>>("tags") ?: emptyList()
        
        // 检查用户是否明确要求加密
        val requiresEncryption = tags.any { 
            it.lowercase().contains("encrypt") || it.lowercase().contains("secure") 
        }
        
        // 私有文件或明确要求加密的文件进行加密
        return !isPublic || requiresEncryption
    }
    
    /**
     * 生成或获取加密密钥
     */
    private fun generateOrGetEncryptionKey(context: ProcessingContext): SecretKey {
        // 在实际实现中，应该从密钥管理服务获取或生成密钥
        // 这里为了演示，生成一个随机密钥
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // AES-256
        return keyGen.generateKey()
    }
    
    /**
     * 生成初始化向量
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(12) // GCM模式推荐12字节IV
        SecureRandom().nextBytes(iv)
        return iv
    }
    
    /**
     * 执行字节数组加密
     */
    private fun encryptBytes(input: ByteArray, context: ProcessingContext): ByteArray {
        return try {
            when (configuration.algorithm) {
                "AES-256-GCM" -> encryptWithAESGCM(input)
                "AES-256-CBC" -> encryptWithAESCBC(input)
                else -> {
                    context.addMetadata("encryptionError", "不支持的加密算法：${configuration.algorithm}")
                    input // 返回原始数据
                }
            }
        } catch (e: Exception) {
            // 加密失败时记录错误并返回原始数据
            context.addMetadata("encryptionError", e.message ?: "加密失败")
            if (configuration.logActivity) {
                println("加密失败：${context.fileName}，错误：${e.message}")
            }
            input
        }
    }
    
    /**
     * AES-GCM加密
     */
    private fun encryptWithAESGCM(input: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv) // 128位认证标签
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec)
        
        val encryptedData = cipher.doFinal(input)
        
        // 将IV和加密数据组合（IV前置）
        val result = ByteArray(iv!!.size + encryptedData.size)
        System.arraycopy(iv!!, 0, result, 0, iv!!.size)
        System.arraycopy(encryptedData, 0, result, iv!!.size, encryptedData.size)
        
        return result
    }
    
    /**
     * AES-CBC加密（模拟实现）
     */
    private fun encryptWithAESCBC(input: ByteArray): ByteArray {
        // 实际实现中应该使用CBC模式，这里简化为GCM
        return encryptWithAESGCM(input)
    }
    
    /**
     * 获取密钥ID（用于密钥管理）
     */
    private fun getKeyId(key: SecretKey?): String {
        if (key == null) return "unknown"
        
        // 实际实现中应该是密钥管理系统中的ID
        // 这里使用密钥的哈希作为ID
        val keyHash = key.encoded.contentHashCode()
        return "key_${keyHash.toString(16)}"
    }
    
    /**
     * 格式化字节数为可读字符串
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
}

/**
 * 加密配置
 */
data class EncryptionConfiguration(
    val algorithm: String = "AES-256-GCM",
    val keySize: Int = 256,
    val logActivity: Boolean = true,
    val forceEncryption: Boolean = false // 是否强制加密所有文件
) 