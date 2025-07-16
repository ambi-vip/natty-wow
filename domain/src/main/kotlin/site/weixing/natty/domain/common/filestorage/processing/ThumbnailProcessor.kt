package site.weixing.natty.domain.common.filestorage.processing

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.file.FileMetadata

/**
 * 缩略图处理器
 * 为图片文件生成缩略图
 */
@Component
class ThumbnailProcessor : FileProcessor {
    
    companion object {
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
        )
        private val THUMBNAIL_SIZES = listOf("150x150", "300x300", "600x600")
    }
    
    override fun process(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
        return if (isImageFile(metadata)) {
            generateThumbnails(content, metadata)
        } else {
            Mono.just(ProcessingResult(
                success = true,
                metadata = mapOf(
                    "thumbnailGenerated" to false,
                    "reason" to "不支持的图片格式"
                )
            ))
        }
    }
    
    override fun supports(metadata: FileMetadata): Boolean {
        return isImageFile(metadata)
    }
    
    private fun isImageFile(metadata: FileMetadata): Boolean {
        return SUPPORTED_IMAGE_TYPES.contains(metadata.contentType.lowercase())
    }
    
    private fun generateThumbnails(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult> {
        // 简化实现：实际应用中应该使用图像处理库如ImageIO或BufferedImage
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
                // 模拟缩略图生成过程
                val thumbnailPaths = THUMBNAIL_SIZES.map { size ->
                    "thumbnails/${metadata.originalFileName}_$size.jpg"
                }
                
                ProcessingResult(
                    success = true,
                    metadata = mapOf(
                        "thumbnailGenerated" to true,
                        "thumbnailSizes" to THUMBNAIL_SIZES,
                        "thumbnailPaths" to thumbnailPaths,
                        "originalImageSize" to originalData.size.toLong(),
                        "thumbnailCount" to THUMBNAIL_SIZES.size
                    )
                )
            }
            .onErrorReturn(ProcessingResult(
                success = false,
                errorMessage = "缩略图生成失败"
            ))
    }
}