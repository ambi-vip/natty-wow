package site.weixing.natty.domain.common.filestorage.processing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import site.weixing.natty.domain.common.filestorage.file.FileMetadata

/**
 * 文件处理器测试
 * 专注于测试处理器的核心业务逻辑
 */
class FileProcessorTest {

    private val dataBufferFactory = DefaultDataBufferFactory.sharedInstance

    /**
     * 测试压缩处理器
     */
    @Test
    fun `compression processor should compress text content`() {
        val processor = CompressionProcessor()
        val originalContent = "This is a test content that should be compressed. ".repeat(100)
        val contentFlux = createDataBufferFlux(originalContent.toByteArray())
        
        val metadata = FileMetadata(
            originalFileName = "test.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = originalContent.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                assertThat(result.metadata["compressed"]).isEqualTo(true)
                assertThat(result.metadata["originalSize"]).isEqualTo(originalContent.length.toLong())
                assertThat(result.metadata["compressedSize"]).isNotNull()
                
                val compressedSize = result.metadata["compressedSize"] as Long
                assertThat(compressedSize).isLessThan(originalContent.length.toLong())
                
                // 验证压缩比
                val compressionRatio = compressedSize.toDouble() / originalContent.length.toDouble()
                assertThat(compressionRatio).isBetween(0.01, 0.9) // 预期有显著压缩

                // 校验压缩后解压内容与原始内容一致
                val processedContentFlux = result.processedContent!!
                val compressedBytes = processedContentFlux.collectList().block()!!.flatMap { buf ->
                    val bytes = ByteArray(buf.readableByteCount())
                    buf.read(bytes)
                    bytes.toList()
                }.toByteArray()
                val decompressedBytes = decompressGzip(compressedBytes)
                val decompressedString = String(decompressedBytes)
                assertThat(decompressedString).isEqualTo(originalContent)
            }
            .verifyComplete()
    }

    /**
     * 测试压缩处理器 - 小文件跳过压缩
     */
    @Test
    fun `compression processor should skip small files`() {
        val processor = CompressionProcessor()
        val smallContent = "Small content" // 小于阈值
        val contentFlux = createDataBufferFlux(smallContent.toByteArray())
        
        val metadata = FileMetadata(
            originalFileName = "small.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = smallContent.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                assertThat(result.metadata["compressed"]).isEqualTo(false)
                assertThat(result.metadata["reason"]).isEqualTo("文件太小，跳过压缩")
            }
            .verifyComplete()
    }

    /**
     * 测试加密处理器
     */
    @Test
    fun `encryption processor should encrypt content`() {
        val processor = EncryptionProcessor()
        val originalContent = "Sensitive data that needs encryption"
        val contentFlux = createDataBufferFlux(originalContent.toByteArray())
        
        val metadata = FileMetadata(
            originalFileName = "secret.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = originalContent.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                assertThat(result.metadata["encrypted"]).isEqualTo(true)
                assertThat(result.metadata["algorithm"]).isEqualTo("AES/GCM/NoPadding")
                assertThat(result.metadata["keyId"]).isNotNull()
                assertThat(result.metadata["iv"]).isNotNull()
                
                // 验证加密后的内容与原始内容不同
                val encryptedSize = result.metadata["encryptedSize"] as Long
                assertThat(encryptedSize).isGreaterThan(originalContent.length.toLong()) // 加密后通常会增大
            }
            .verifyComplete()
    }

    /**
     * 测试缩略图处理器
     */
    @Test
    fun `thumbnail processor should generate thumbnail for images`() {
        val processor = ThumbnailProcessor()
        val imageContent = createMockImageContent()
        val contentFlux = createDataBufferFlux(imageContent)
        
        val metadata = FileMetadata(
            originalFileName = "photo.jpg",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = imageContent.size.toLong(),
            contentType = "image/jpeg",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                assertThat(result.metadata["thumbnailGenerated"]).isEqualTo(true)
                assertThat(result.metadata["thumbnailSizes"]).isNotNull()
                
                @Suppress("UNCHECKED_CAST")
                val thumbnailSizes = result.metadata["thumbnailSizes"] as List<String>
                assertThat(thumbnailSizes).contains("150x150", "300x300", "600x600")
                
                // 验证缩略图路径
                assertThat(result.metadata["thumbnailPaths"]).isNotNull()
            }
            .verifyComplete()
    }

    /**
     * 测试缩略图处理器 - 非图片文件
     */
    @Test
    fun `thumbnail processor should skip non-image files`() {
        val processor = ThumbnailProcessor()
        val textContent = "This is not an image file"
        val contentFlux = createDataBufferFlux(textContent.toByteArray())
        
        val metadata = FileMetadata(
            originalFileName = "document.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = textContent.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                assertThat(result.metadata["thumbnailGenerated"]).isEqualTo(false)
                assertThat(result.metadata["reason"]).isEqualTo("不支持的图片格式")
            }
            .verifyComplete()
    }

    /**
     * 测试处理协调器
     */
    @Test
    fun `processing coordinator should orchestrate multiple processors`() {
        val coordinator = ProcessingCoordinator(
            listOf(
                CompressionProcessor(),
                EncryptionProcessor()
            )
        )
        
        val content = "Test content for processing pipeline"
        val contentFlux = createDataBufferFlux(content.toByteArray())
        
        val options = ProcessingOptions(
            requireEncryption = true,
            enableCompression = true,
            generateThumbnail = false
        )
        
        val metadata = FileMetadata(
            originalFileName = "test.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = content.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(coordinator.processFile(contentFlux, options, metadata))
            .assertNext { result ->
                assertThat(result.success).isTrue()
                
                // 验证所有启用的处理器都被执行
                assertThat(result.metadata["processingSteps"]).isNotNull()
                
                @Suppress("UNCHECKED_CAST")
                val steps = result.metadata["processingSteps"] as List<String>
                val property = CompressionProcessor::class::simpleName
                assertThat(steps).contains("CompressionProcessor", "EncryptionProcessor")
                
                // 验证处理指标
                assertThat(result.metadata["totalProcessingTime"]).isNotNull()
                assertThat(result.metadata["processedBy"]).isNotNull()
            }
            .verifyComplete()
    }

    /**
     * 测试处理选项验证
     */
    @Test
    fun `processing options should validate requirements correctly`() {
        // 测试需要处理的选项
        val optionsWithProcessing = ProcessingOptions(
            requireEncryption = true,
            enableCompression = false,
            generateThumbnail = false
        )
        assertThat(optionsWithProcessing.requiresProcessing()).isTrue()
        
        // 测试不需要处理的选项
        val optionsWithoutProcessing = ProcessingOptions(
            requireEncryption = false,
            enableCompression = false,
            generateThumbnail = false
        )
        assertThat(optionsWithoutProcessing.requiresProcessing()).isFalse()
        
        // 测试自定义处理器
        val optionsWithCustom = ProcessingOptions(
            requireEncryption = false,
            enableCompression = false,
            generateThumbnail = false,
            customProcessors = listOf("watermark")
        )
        assertThat(optionsWithCustom.requiresProcessing()).isTrue()
    }

    /**
     * 测试处理器错误处理
     */
    @Test
    fun `processor should handle errors gracefully`() {
        val processor = object : FileProcessor {
            override fun process(content: Flux<DataBuffer>, metadata: FileMetadata): reactor.core.publisher.Mono<ProcessingResult> {
                return reactor.core.publisher.Mono.error(RuntimeException("Processing failed"))
            }
            
            override fun supports(metadata: FileMetadata): Boolean = true
        }
        
        val content = "Test content"
        val contentFlux = createDataBufferFlux(content.toByteArray())
        
        val metadata = FileMetadata(
            originalFileName = "test.txt",
            uploaderId = "user-123",
            folderId = "folder-456",
            fileSize = content.length.toLong(),
            contentType = "text/plain",
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            uploadTimestamp = System.currentTimeMillis()
        )

        StepVerifier.create(processor.process(contentFlux, metadata))
            .expectError(RuntimeException::class.java)
            .verify()
    }

    /**
     * 创建数据缓冲区流
     */
    private fun createDataBufferFlux(content: ByteArray): Flux<DataBuffer> {
        val buffer = dataBufferFactory.wrap(content)
        return Flux.just(buffer)
    }

    /**
     * 创建模拟图片内容
     */
    private fun createMockImageContent(): ByteArray {
        // 简化的JPEG文件头
        return byteArrayOf(
            -1, -40, -1, -32, // JPEG SOI and JFIF markers
            0, 16, 74, 70, 73, 70, 0, 1, // JFIF header
            1, 1, 0, 72, 0, 72, 0, 0, // version and density
            *ByteArray(100) { (it % 256).toByte() } // mock image data
        )
    }

    // 辅助方法：GZIP解压
    private fun decompressGzip(compressed: ByteArray): ByteArray {
        java.util.zip.GZIPInputStream(compressed.inputStream()).use { gzipIn ->
            return gzipIn.readAllBytes()
        }
    }
}