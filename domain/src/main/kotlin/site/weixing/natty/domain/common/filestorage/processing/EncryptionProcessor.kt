package site.weixing.natty.domain.common.filestorage.processing

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.file.FileMetadata
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密处理器
 * 使用AES-256-GCM算法进行文件加密
 */
@Component
class EncryptionProcessor : FileProcessor {
    
    companion object {
        private val dataBufferFactory = DefaultDataBufferFactory.sharedInstance
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    override fun process(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
        return performEncryption(content, metadata)
    }
    
    override fun supports(metadata: FileMetadata): Boolean {
        // 支持所有文件类型的加密
        return true
    }
    
    private fun performEncryption(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
        return content.collectList()
            .map { buffers ->
                val originalData = ByteArray(buffers.sumOf { it.readableByteCount() })
                var offset = 0
                
                buffers.forEach { buffer ->
                    val bytes = ByteArray(buffer.readableByteCount())
                    buffer.read(bytes)
                    System.arraycopy(bytes, 0, originalData, offset, bytes.size)
                    offset += bytes.size
                }
                
                originalData
            }
            .map { originalData ->
                // 生成密钥
                val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
                keyGenerator.init(256)
                val secretKey = keyGenerator.generateKey()
                
                // 生成IV
                val iv = ByteArray(GCM_IV_LENGTH)
                SecureRandom().nextBytes(iv)
                
                // 执行加密
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)
                
                val encryptedData = cipher.doFinal(originalData)
                
                // 生成密钥ID（实际应用中应该安全存储密钥）
                val keyId = UUID.randomUUID().toString()
                val encodedKey = Base64.getEncoder().encodeToString(secretKey.encoded)
                val encodedIv = Base64.getEncoder().encodeToString(iv)
                
                ProcessingResult(
                    success = true,
                    metadata = mapOf(
                        "encrypted" to true,
                        "algorithm" to TRANSFORMATION,
                        "keyId" to keyId,
                        "key" to encodedKey, // 注意：实际应用中不应该在元数据中存储密钥
                        "iv" to encodedIv,
                        "originalSize" to originalData.size.toLong(),
                        "encryptedSize" to encryptedData.size.toLong(),
                        "processedContent" to Flux.just(dataBufferFactory.wrap(encryptedData))
                    )
                )
            }
            .onErrorReturn(ProcessingResult(
                success = false,
                errorMessage = "加密处理失败"
            ))
    }
}