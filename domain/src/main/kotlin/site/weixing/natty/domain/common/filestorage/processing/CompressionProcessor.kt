package site.weixing.natty.domain.common.filestorage.processing

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.file.FileMetadata
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/**
 * 压缩处理器
 * 使用GZIP算法进行文件压缩
 */
@Component
class CompressionProcessor : FileProcessor {
    
    companion object {
        private val COMPRESSION_THRESHOLD = 1024L // 1KB阈值
        private val dataBufferFactory = DefaultDataBufferFactory.sharedInstance
    }
    
    override fun process(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
        return if (shouldCompress(metadata)) {
            performCompression(content, metadata)
        } else {
            Mono.just(ProcessingResult(
                success = true,
                metadata = mapOf(
                    "compressed" to false,
                    "reason" to "文件太小，跳过压缩"
                )
            ))
        }
    }
    
    override fun supports(metadata: FileMetadata): Boolean {
        // 支持文本文件和某些二进制文件的压缩
        return metadata.contentType.startsWith("text/") ||
               metadata.contentType == "application/json" ||
               metadata.contentType == "application/xml" ||
               metadata.fileSize > COMPRESSION_THRESHOLD
    }
    
    private fun shouldCompress(metadata: FileMetadata): Boolean {
        return metadata.fileSize > COMPRESSION_THRESHOLD && 
               !metadata.contentType.startsWith("image/") && // 图片通常已压缩
               !metadata.contentType.startsWith("video/") && // 视频通常已压缩
               !metadata.contentType.contains("zip") &&      // 已压缩格式
               !metadata.contentType.contains("gz")          // 已压缩格式
    }
    
    private fun performCompression(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
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
                val outputStream = ByteArrayOutputStream()
                GZIPOutputStream(outputStream).use { gzipOut ->
                    gzipOut.write(originalData)
                }
                
                val compressedData = outputStream.toByteArray()
                val compressionRatio = compressedData.size.toDouble() / originalData.size.toDouble()
                
                ProcessingResult(
                    success = true,
                    metadata = mapOf(
                        "compressed" to true,
                        "originalSize" to originalData.size.toLong(),
                        "compressedSize" to compressedData.size.toLong(),
                        "compressionRatio" to compressionRatio,
                        "algorithm" to "GZIP"
                    ),
                    processedContent = Flux.just(dataBufferFactory.wrap(compressedData))
                )
            }
            .onErrorReturn(ProcessingResult(
                success = false,
                errorMessage = "压缩处理失败"
            ))
    }
}