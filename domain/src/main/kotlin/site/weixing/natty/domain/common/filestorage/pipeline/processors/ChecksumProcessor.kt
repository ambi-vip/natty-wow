package site.weixing.natty.domain.common.filestorage.pipeline.processors

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessorStatistics
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * 校验和计算流处理器
 * 在文件流过程中计算SHA-256校验和，不修改流内容
 */
class ChecksumProcessor(
    private val algorithm: String = "SHA-256"
) : StreamProcessor {
    
    override val name: String = "ChecksumProcessor"
    override val priority: Int = 10 // 高优先级，尽早计算
    
    private var digest: MessageDigest? = null
    private var processedBytes: Long = 0
    private var startTime: Long = 0
    
    override fun isApplicable(context: ProcessingContext): Boolean {
        return context.processingOptions.enableChecksumValidation
    }
    
    override fun initialize(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            digest = MessageDigest.getInstance(algorithm)
            processedBytes = 0
            startTime = System.currentTimeMillis()
        }
    }
    
    override fun process(input: Flux<ByteBuffer>, context: ProcessingContext): Flux<ByteBuffer> {
        return input.map { buffer ->
            // 复制buffer用于校验和计算，避免修改原始buffer
            val duplicate = buffer.duplicate()
            
            // 计算校验和
            digest?.let { digest ->
                val bytes = ByteArray(duplicate.remaining())
                duplicate.get(bytes)
                digest.update(bytes)
                processedBytes += bytes.size
            }
            
            // 返回原始buffer，不修改流内容
            buffer
        }
    }
    
    override fun cleanup(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            digest?.let { digest ->
                // 计算最终校验和
                val checksum = bytesToHex(digest.digest())
                
                // 将校验和存储到上下文中
                context.addMetadata("checksum", checksum)
                context.addMetadata("checksumAlgorithm", algorithm)
                
                // 清理资源
                this.digest = null
            }
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
     * 将字节数组转换为十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
} 