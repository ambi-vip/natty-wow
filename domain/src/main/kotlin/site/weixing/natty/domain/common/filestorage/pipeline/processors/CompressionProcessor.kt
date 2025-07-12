package site.weixing.natty.domain.common.filestorage.pipeline.processors

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessorStatistics
import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Deflater
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory

/**
 * 压缩流处理器
 * 对文件内容进行流式压缩，支持多种压缩算法
 */
class CompressionProcessor(
    private val configuration: CompressionConfiguration = CompressionConfiguration()
) : StreamProcessor {
    
    override val name: String = "CompressionProcessor"
    override val priority: Int = 30 // 中等优先级，在安全检查之后
    
    private var originalSize: Long = 0
    private var compressedSize: Long = 0
    private var startTime: Long = 0
    private var shouldCompress: Boolean = false
    
    override fun isApplicable(context: ProcessingContext): Boolean {
        return context.processingOptions.enableCompression && 
               shouldCompressFile(context)
    }
    
    override fun initialize(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            originalSize = 0
            compressedSize = 0
            startTime = System.currentTimeMillis()
            shouldCompress = shouldCompressFile(context)
            
            if (configuration.logActivity) {
                println("压缩处理器初始化：${context.fileName}，算法：${configuration.algorithm}")
            }
        }
    }
    
    override fun process(input: Flux<DataBuffer>, context: ProcessingContext): Flux<DataBuffer> {
        return if (!shouldCompress) {
            input.map { dataBuffer ->
                originalSize += dataBuffer.readableByteCount()
                compressedSize = originalSize
                dataBuffer
            }.doOnComplete {
                recordCompressionResult(context, shouldCompress)
            }
        } else {
            input
                .collectList()
                .flatMapMany { buffers ->
                    originalSize = buffers.sumOf { it.readableByteCount().toLong() }
                    val totalSize = buffers.sumOf { it.readableByteCount() }
                    val inputBytes = ByteArray(totalSize)
                    var offset = 0
                    buffers.forEach { buffer ->
                        val len = buffer.readableByteCount()
                        buffer.read(inputBytes, offset, len)
                        offset += len
                    }
                    val compressedBytes = compressBytes(inputBytes, context)
                    compressedSize = compressedBytes.size.toLong()
                    val factory = DefaultDataBufferFactory()
                    Flux.fromIterable(listOf(factory.wrap(compressedBytes))) as Flux<DataBuffer>
                }
                .doOnComplete {
                    recordCompressionResult(context, shouldCompress)
                }
        }
    }
    
    /**
     * 记录压缩结果到上下文
     */
    private fun recordCompressionResult(context: ProcessingContext, wasCompressed: Boolean) {
        val compressionRatio = if (originalSize > 0) {
            compressedSize.toDouble() / originalSize.toDouble()
        } else 1.0
        
        context.addMetadata("compression_algorithm", configuration.algorithm.name)
        context.addMetadata("original_size", originalSize)
        context.addMetadata("compressed_size", compressedSize)
        context.addMetadata("compression_ratio", compressionRatio)
        context.addMetadata("space_saved_bytes", originalSize - compressedSize)
        context.addMetadata("compressed", wasCompressed)
        
        if (configuration.logActivity) {
            val status = if (wasCompressed) "压缩完成" else "跳过压缩"
            println("$status：${context.fileName}，" +
                   "原始大小：${formatBytes(originalSize)}，" +
                   "处理后：${formatBytes(compressedSize)}，" +
                   "压缩比：${"%.2f".format(compressionRatio)}")
        }
    }
    
    override fun cleanup(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            // 清理压缩器资源
            this.originalSize = 0
            this.compressedSize = 0
        }
    }
    
    override fun getStatistics(): ProcessorStatistics {
        val endTime = System.currentTimeMillis()
        val processingTime = if (startTime > 0) endTime - startTime else 0
        
        return ProcessorStatistics(
            processorName = name,
            processedFiles = if (originalSize > 0) 1 else 0,
            totalProcessingTime = processingTime,
            averageProcessingTime = processingTime.toDouble(),
            errorCount = 0,
            bytesProcessed = originalSize,
            lastProcessingTime = processingTime
        )
    }
    
    /**
     * 判断文件是否应该压缩
     */
    private fun shouldCompressFile(context: ProcessingContext): Boolean {
        // 文件大小小于阈值不压缩
        if (context.fileSize < configuration.minimumSizeThreshold) {
            return false
        }
        
        // 已经是压缩格式的文件不再压缩
        val preCompressedTypes = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/avi",
            "audio/mp3", "audio/aac", "audio/ogg",
            "application/zip", "application/gzip", "application/x-rar"
        )
        
        if (preCompressedTypes.contains(context.contentType)) {
            return false
        }
        
        // 检查文件扩展名
        val fileName = context.fileName.lowercase()
        val preCompressedExtensions = setOf(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".mp4", ".avi", ".mov", ".webm",
            ".mp3", ".aac", ".ogg", ".flac",
            ".zip", ".rar", ".7z", ".gz", ".bz2"
        )
        
        if (preCompressedExtensions.any { fileName.endsWith(it) }) {
            return false
        }
        
        return true
    }
    
    /**
     * 执行字节数组压缩
     */
    private fun compressBytes(input: ByteArray, context: ProcessingContext): ByteArray {
        return try {
            when (configuration.algorithm) {
                CompressionAlgorithm.GZIP -> compressWithGzip(input)
                CompressionAlgorithm.DEFLATE -> compressWithDeflate(input)
                CompressionAlgorithm.ZSTD -> compressWithZstd(input) // 模拟实现
            }
        } catch (e: Exception) {
            // 压缩失败时记录错误并返回原始数据
            context.addMetadata("compressionError", e.message ?: "压缩失败")
            if (configuration.logActivity) {
                println("压缩失败：${context.fileName}，错误：${e.message}")
            }
            input
        }
    }
    
    /**
     * GZIP压缩
     */
    private fun compressWithGzip(input: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipStream ->
            gzipStream.write(input)
        }
        return outputStream.toByteArray()
    }
    
    /**
     * Deflate压缩
     */
    private fun compressWithDeflate(input: ByteArray): ByteArray {
        val deflater = Deflater(configuration.compressionLevel)
        deflater.setInput(input)
        deflater.finish()
        
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        
        deflater.end()
        return outputStream.toByteArray()
    }
    
    /**
     * ZSTD压缩（模拟实现）
     */
    private fun compressWithZstd(input: ByteArray): ByteArray {
        // 实际实现中应该使用ZSTD库，这里模拟压缩效果
        return compressWithDeflate(input)
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
 * 压缩配置
 */
data class CompressionConfiguration(
    val algorithm: CompressionAlgorithm = CompressionAlgorithm.GZIP,
    val compressionLevel: Int = 6, // 1-9，6为默认平衡值
    val minimumSizeThreshold: Long = 1024L, // 1KB，小于此大小不压缩
    val logActivity: Boolean = true
)

/**
 * 压缩算法枚举
 */
enum class CompressionAlgorithm {
    GZIP,
    DEFLATE,
    ZSTD
} 