package site.weixing.natty.domain.common.filestorage.pipeline.processors

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessorStatistics
import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.RenderingHints

/**
 * 缩略图生成流处理器
 * 为图片文件生成缩略图，不修改原始流内容，将缩略图信息添加到上下文
 */
class ThumbnailProcessor(
    private val configuration: ThumbnailConfiguration = ThumbnailConfiguration()
) : StreamProcessor {
    
    override val name: String = "ThumbnailProcessor"
    override val priority: Int = 50 // 低优先级，在其他处理之后
    
    private var processedBytes: Long = 0
    private var startTime: Long = 0
    private var thumbnailGenerated: Boolean = false
    private var thumbnailData: ByteArray? = null
    
    override fun isApplicable(context: ProcessingContext): Boolean {
        return context.processingOptions.enableThumbnail && 
               isImageFile(context)
    }
    
    override fun initialize(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            processedBytes = 0
            startTime = System.currentTimeMillis()
            thumbnailGenerated = false
            thumbnailData = null
            
            if (configuration.logActivity) {
                println("缩略图处理器初始化：${context.fileName}")
            }
        }
    }
    
    override fun process(input: Flux<ByteBuffer>, context: ProcessingContext): Flux<ByteBuffer> {
        return input
            .collectList() // 收集所有数据进行缩略图生成
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
                
                // 生成缩略图
                generateThumbnail(inputBytes, context)
                
                // 返回原始数据，不修改流内容
                Flux.fromIterable(buffers)
            }
            .doOnComplete {
                // 记录缩略图信息到上下文
                if (thumbnailGenerated && thumbnailData != null) {
                    context.addMetadata("thumbnailGenerated", true)
                    context.addMetadata("thumbnailSize", configuration.size)
                    context.addMetadata("thumbnailFormat", configuration.format)
                    context.addMetadata("thumbnailDataSize", thumbnailData!!.size)
                    context.addMetadata("thumbnails", "generated:${configuration.size}x${configuration.size}")
                    // 在实际实现中，可能需要将缩略图数据存储到单独的位置
                    // context.addMetadata("thumbnailData", Base64.getEncoder().encodeToString(thumbnailData!!))
                } else {
                    context.addMetadata("thumbnailGenerated", false)
                    context.addMetadata("thumbnails", "skipped")
                }
                
                if (configuration.logActivity) {
                    if (thumbnailGenerated) {
                        println("缩略图生成完成：${context.fileName}，尺寸：${configuration.size}x${configuration.size}")
                    } else {
                        println("缩略图生成跳过：${context.fileName}（不是支持的图片格式）")
                    }
                }
            }
    }
    
    override fun cleanup(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            // 清理缩略图处理器资源
            this.thumbnailData = null
            this.thumbnailGenerated = false
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
            errorCount = if (!thumbnailGenerated && processedBytes > 0) 1 else 0,
            bytesProcessed = processedBytes,
            lastProcessingTime = processingTime
        )
    }
    
    /**
     * 检查是否为图片文件
     */
    private fun isImageFile(context: ProcessingContext): Boolean {
        val imageContentTypes = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/bmp", "image/webp", "image/tiff"
        )
        
        if (imageContentTypes.contains(context.contentType)) {
            return true
        }
        
        // 检查文件扩展名
        val fileName = context.fileName.lowercase()
        val imageExtensions = setOf(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif"
        )
        
        return imageExtensions.any { fileName.endsWith(it) }
    }
    
    /**
     * 生成缩略图
     */
    private fun generateThumbnail(imageData: ByteArray, context: ProcessingContext) {
        try {
            // 读取原始图片
            val inputStream = ByteArrayInputStream(imageData)
            val originalImage = ImageIO.read(inputStream)
            
            if (originalImage == null) {
                if (configuration.logActivity) {
                    println("无法读取图片数据：${context.fileName}")
                }
                return
            }
            
            // 检查是否需要生成缩略图
            if (!shouldGenerateThumbnail(originalImage, context)) {
                return
            }
            
            // 计算缩略图尺寸
            val thumbnailSize = calculateThumbnailSize(originalImage)
            
            // 创建缩略图
            val thumbnail = createThumbnail(originalImage, thumbnailSize.width, thumbnailSize.height)
            
            // 转换为字节数组
            thumbnailData = imageToByteArray(thumbnail, configuration.format)
            thumbnailGenerated = true
            
            // 添加详细信息到上下文
            context.addMetadata("originalImageWidth", originalImage.width)
            context.addMetadata("originalImageHeight", originalImage.height)
            context.addMetadata("thumbnailWidth", thumbnail.width)
            context.addMetadata("thumbnailHeight", thumbnail.height)
            
        } catch (e: Exception) {
            // 缩略图生成失败
            context.addMetadata("thumbnailError", e.message ?: "缩略图生成失败")
            if (configuration.logActivity) {
                println("缩略图生成失败：${context.fileName}，错误：${e.message}")
            }
        }
    }
    
    /**
     * 判断是否需要生成缩略图
     */
    private fun shouldGenerateThumbnail(image: BufferedImage, context: ProcessingContext): Boolean {
        // 图片太小不需要缩略图
        if (image.width <= configuration.size && image.height <= configuration.size) {
            context.addMetadata("thumbnailSkipReason", "图片尺寸小于缩略图尺寸")
            return false
        }
        
        // 检查文件大小限制
        if (context.fileSize > configuration.maxFileSizeBytes) {
            context.addMetadata("thumbnailSkipReason", "文件大小超过限制")
            return false
        }
        
        // 检查图片尺寸限制
        if (image.width > configuration.maxImageWidth || image.height > configuration.maxImageHeight) {
            context.addMetadata("thumbnailSkipReason", "图片尺寸过大")
            return false
        }
        
        return true
    }
    
    /**
     * 计算缩略图尺寸（保持宽高比）
     */
    private fun calculateThumbnailSize(image: BufferedImage): ThumbnailSize {
        val originalWidth = image.width
        val originalHeight = image.height
        val targetSize = configuration.size
        
        if (originalWidth <= targetSize && originalHeight <= targetSize) {
            return ThumbnailSize(originalWidth, originalHeight)
        }
        
        val widthRatio = targetSize.toDouble() / originalWidth
        val heightRatio = targetSize.toDouble() / originalHeight
        val scaleFactor = minOf(widthRatio, heightRatio)
        
        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()
        
        return ThumbnailSize(newWidth, newHeight)
    }
    
    /**
     * 创建缩略图
     */
    private fun createThumbnail(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
        val thumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = thumbnail.createGraphics()
        
        // 设置高质量渲染
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // 绘制缩放后的图片
        val scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        graphics.drawImage(scaledImage, 0, 0, null)
        graphics.dispose()
        
        return thumbnail
    }
    
    /**
     * 将图片转换为字节数组
     */
    private fun imageToByteArray(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }
}

/**
 * 缩略图配置
 */
data class ThumbnailConfiguration(
    val size: Int = 256, // 缩略图尺寸（正方形）
    val format: String = "JPEG", // 缩略图格式
    val quality: Float = 0.8f, // 压缩质量 (0.0-1.0)
    val maxFileSizeBytes: Long = 50 * 1024 * 1024L, // 50MB，超过此大小不生成缩略图
    val maxImageWidth: Int = 10000, // 最大图片宽度
    val maxImageHeight: Int = 10000, // 最大图片高度
    val logActivity: Boolean = true
)

/**
 * 缩略图尺寸数据类
 */
data class ThumbnailSize(
    val width: Int,
    val height: Int
) 