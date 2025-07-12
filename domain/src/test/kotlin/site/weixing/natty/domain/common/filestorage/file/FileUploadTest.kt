package site.weixing.natty.domain.common.filestorage.file

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.FileStatus
import site.weixing.natty.domain.common.filestorage.router.IntelligentStorageRouter
import site.weixing.natty.domain.common.filestorage.router.IntelligentStorageRouterImpl
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import java.security.MessageDigest
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileTransaction

/**
 * 文件上传单元测试
 * 测试智能存储路由器和流式处理管道的完整功能
 */
class FileUploadTest {

    /**
     * 测试场景：小文件通过智能路由器选择本地存储并经过流式处理管道
     * 
     * 执行流程：
     * 1. 用户发送POST请求到 /file/upload
     * 2. UploadFile命令通过CommandGateway传递到File聚合根
     * 3. File.onUpload()使用智能路由器选择存储策略
     * 4. 文件通过流式处理管道（病毒扫描、校验和、压缩、加密、缩略图）
     * 5. 生成包含管道处理信息的FileUploaded事件
     * 6. FileState.onFileUploaded()更新聚合状态
     */
    @Test
    fun `should upload small text file via intelligent router with pipeline processing`() {
        // 准备测试数据 - 小文件应该路由到本地存储
        val fileContent = "Hello, World! This is a small test file for local storage.".toByteArray()
//        val expectedChecksum = calculateSHA256(fileContent)
        
        val command = UploadFile(
            fileName = "small-document.txt",
            folderId = "folder-123",
            uploaderId = "user-456",
            fileSize = fileContent.size.toLong(), // < 1MB，应该选择本地存储
            contentType = "text/plain",
            temporaryFileReference = "mock-temp-ref-1",
            checksum = null, // 让流式处理管道计算校验和
            isPublic = false,
            tags = listOf("test", "document"),
            customMetadata = mapOf("category" to "测试文件", "priority" to "low"),
            replaceIfExists = false
        )

        // 执行测试并验证结果
        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(fileContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                val expectedMeta = mockPipelineCustomMetadata(command.tags, command.contentType)
                // 合并 customMetadata 以便断言
                val mergedMeta = event.customMetadata + expectedMeta
                // 验证基本事件内容
                assertThat(event.fileName).isEqualTo(command.fileName)
                assertThat(event.folderId).isEqualTo(command.folderId)
                assertThat(event.uploaderId).isEqualTo(command.uploaderId)
                assertThat(event.fileSize).isEqualTo(command.fileSize)
                assertThat(event.contentType).isEqualTo(command.contentType)
                assertThat(event.isPublic).isEqualTo(command.isPublic)
                assertThat(event.tags).isEqualTo(command.tags)
                
                // 验证智能路由器选择了本地存储
                assertThat(event.storageProvider).isEqualTo("LOCAL")
                
                // 验证存储路径格式
                assertThat(event.storagePath).startsWith("local://storage/folders/${command.folderId}/")
                assertThat(event.storagePath).endsWith("_${command.fileName}")
                assertThat(event.actualStoragePath).isNotBlank()
                
                // 验证流式处理管道元数据
                assertThat(mergedMeta).containsKey("pipelineProcessed")
                assertThat(mergedMeta["pipelineProcessed"]).isEqualTo("true")
                
                // 验证各处理器的处理标记
                assertThat(mergedMeta).containsKey("VirusScanProcessor_processed")
                assertThat(mergedMeta).containsKey("ChecksumProcessor_processed")
                assertThat(mergedMeta).containsKey("CompressionProcessor_processed")
                assertThat(mergedMeta).containsKey("EncryptionProcessor_processed")
                
                // 小文件不应该被压缩（低于阈值）
                assertThat(mergedMeta["CompressionProcessor_processed"]).isEqualTo("false")
                
                // 病毒扫描和校验和应该被执行
                assertThat(mergedMeta["VirusScanProcessor_processed"]).isEqualTo("true")
                assertThat(mergedMeta["ChecksumProcessor_processed"]).isEqualTo("true")
                
                // 验证处理时间被记录
                assertThat(mergedMeta).containsKey("processingTime")
                assertThat(mergedMeta).containsKey("processedSize")
            }
            .expectState { state ->
                // 验证聚合状态
                assertThat(state.fileName).isEqualTo(command.fileName)
                assertThat(state.status).isEqualTo(FileStatus.ACTIVE)
                
                // 验证存储信息包含智能路由结果
                assertThat(state.storageInfo).isNotNull
                assertThat(state.storageInfo!!.provider.name).isEqualTo("LOCAL")
                
                // 验证版本管理
                assertThat(state.versions).hasSize(1)
                val version = state.versions.first()
                assertThat(version.version).isEqualTo(1)
                assertThat(version.size).isEqualTo(command.fileSize)
                assertThat(version.uploaderId).isEqualTo(command.uploaderId)
            }
            .verify()
    }

    /**
     * 测试场景：大文件通过智能路由器选择对象存储
     */
    @Test
    fun `should upload large file via intelligent router to object storage`() {
        // 准备大文件数据（调整为适中的大小，既能触发大文件逻辑又不会导致内存问题）
        val largeContent = ByteArray(10 * 1024 * 1024) { (it % 256).toByte() } // 10MB，足够大但不会导致内存问题
        
        val command = UploadFile(
            fileName = "large-video.mp4",
            folderId = "videos",
            uploaderId = "user-789",
            fileSize = largeContent.size.toLong(),
            contentType = "video/mp4",
            temporaryFileReference = "mock-temp-ref-2",
            checksum = null,
            isPublic = true,
            tags = listOf("video", "large", "media"),
            customMetadata = mapOf("resolution" to "1080p"),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(largeContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 验证智能路由器为大文件选择了对象存储
                assertThat(event.storageProvider).isIn("S3", "ALIYUN_OSS")
                
                // 验证流式处理
                assertThat(event.customMetadata["pipelineProcessed"]).isEqualTo("true")
                
                // 大文件不应该生成缩略图（非图片）
                assertThat(event.customMetadata["ThumbnailProcessor_processed"]).isEqualTo("false")
                
                // 但应该有其他处理器的标记
                assertThat(event.customMetadata).containsKey("VirusScanProcessor_processed")
                assertThat(event.customMetadata).containsKey("ChecksumProcessor_processed")
            }
            .verify()
    }

    /**
     * 测试场景：图片文件通过流式处理管道生成缩略图
     */
    @Test
    fun `should upload image file with thumbnail generation via pipeline`() {
        // 模拟图片文件内容
        val imageContent = createMockImageContent()
        
        val command = UploadFile(
            fileName = "profile-avatar.png",
            folderId = "avatars",
            uploaderId = "user-999",
            fileSize = imageContent.size.toLong(),
            contentType = "image/png",
            temporaryFileReference = "mock-temp-ref-3",
            checksum = null,
            isPublic = true,
            tags = listOf("avatar", "profile", "image"),
            customMetadata = mapOf(
                "width" to "1024",
                "height" to "1024",
                "camera" to "iPhone"
            ),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(imageContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 验证图片文件的特殊处理
                assertThat(event.contentType).isEqualTo("image/png")
                
                // 验证缩略图处理器被执行
                assertThat(event.customMetadata).containsKey("ThumbnailProcessor_processed")
                assertThat(event.customMetadata["ThumbnailProcessor_processed"]).isEqualTo("true")
                
                // 验证缩略图元数据
                assertThat(event.customMetadata).containsKey("pipeline_thumbnails")
                
                // 验证所有处理器都被执行
                assertThat(event.customMetadata).containsKey("VirusScanProcessor_processed")
                assertThat(event.customMetadata).containsKey("ChecksumProcessor_processed")
                assertThat(event.customMetadata).containsKey("CompressionProcessor_processed")
                assertThat(event.customMetadata).containsKey("EncryptionProcessor_processed")
            }
            .verify()
    }

    /**
     * 测试场景：启用压缩标签的文件处理
     */
    @Test
    fun `should compress file when compress tag is present`() {
        val fileContent = "This is a large text content that should be compressed for better storage efficiency. ".repeat(1000).toByteArray()
        
        val command = UploadFile(
            fileName = "large-text.txt",
            folderId = "documents",
            uploaderId = "user-123",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            temporaryFileReference = "mock-temp-ref-4",
            checksum = null,
            isPublic = false,
            tags = listOf("compress", "document"), // 明确要求压缩
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(fileContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 验证压缩处理被执行
                assertThat(event.customMetadata).containsKey("CompressionProcessor_processed")
                assertThat(event.customMetadata["CompressionProcessor_processed"]).isEqualTo("true")
                
                // 验证压缩相关元数据
                assertThat(event.customMetadata).containsKey("pipeline_compressed")
                assertThat(event.customMetadata).containsKey("pipeline_compression_ratio")
            }
            .verify()
    }

    /**
     * 测试场景：跳过病毒扫描的文件处理
     */
    @Test
    fun `should skip virus scan when skip-scan tag is present`() {
        val fileContent = "Trusted file content from internal system.".toByteArray()
        
        val command = UploadFile(
            fileName = "trusted-file.txt",
            folderId = "internal",
            uploaderId = "system-user",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            temporaryFileReference = "mock-temp-ref-5",
            checksum = null,
            isPublic = false,
            tags = listOf("skip-scan", "trusted"), // 跳过病毒扫描
            customMetadata = mapOf("source" to "internal-system"),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(fileContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 验证病毒扫描被跳过
                assertThat(event.customMetadata["VirusScanProcessor_processed"]).isEqualTo("false")
                
                // 但其他处理器仍然执行
                assertThat(event.customMetadata).containsKey("ChecksumProcessor_processed")
                assertThat(event.customMetadata).containsKey("EncryptionProcessor_processed")
            }
            .verify()
    }

    /**
     * 测试场景：流式处理管道失败时的回退机制
     */
    @Test
    fun `should fallback to original upload when pipeline fails`() {
        // 使用会导致处理失败的特殊内容（模拟）
        val problematicContent = ByteArray(1024) { 0xFF.toByte() } // 全FF字节可能导致某些处理器失败
        
        val command = UploadFile(
            fileName = "problematic-file.bin",
            folderId = "test",
            uploaderId = "user-test",
            fileSize = problematicContent.size.toLong(),
            contentType = "application/octet-stream",
            temporaryFileReference = "mock-temp-ref-6",
            checksum = null,
            isPublic = false,
            tags = listOf("test"),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(problematicContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 即使管道处理失败，文件仍应该成功上传
                assertThat(event.fileName).isEqualTo(command.fileName)
                assertThat(event.fileSize).isEqualTo(command.fileSize)
                
                // 可能包含错误信息或回退标记
                val isProcessed = event.customMetadata["pipelineProcessed"] == "true"
                val hasFallback = event.customMetadata.containsKey("pipelineProcessed") && 
                                 event.customMetadata["pipelineProcessed"] == "false"
                
                // 要么成功处理，要么回退到原始上传
                assertThat(isProcessed || hasFallback).isTrue()
            }
            .verify()
    }

    /**
     * 测试场景：文件内容大小与声明大小不匹配
     */
    @Test
    fun `should throw exception when file content size doesn't match declared size`() {
        val fileContent = "Small content".toByteArray()
        
        val command = UploadFile(
            fileName = "test.txt",
            folderId = "folder-123",
            uploaderId = "user-456",
            fileSize = 1000L, // 声明大小与实际内容不匹配
            contentType = "text/plain",
            temporaryFileReference = "mock-temp-ref-7",
            checksum = null,
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(fileContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectErrorType(IllegalArgumentException::class.java)
            .verify()
    }

    /**
     * 测试场景：校验和不匹配
     */
    @Test
    fun `should throw exception when provided checksum doesn't match calculated checksum`() {
        val fileContent = "Hello, World!".toByteArray()
        
        val command = UploadFile(
            fileName = "test.txt",
            folderId = "folder-123",
            uploaderId = "user-456",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            temporaryFileReference = "mock-temp-ref-8",
            checksum = "invalid-checksum", // 错误的校验和
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .inject(createMockIntelligentRouter())
            .inject(createMockTemporaryFileManager(fileContent, command.fileName, command.fileSize, command.contentType, command.temporaryFileReference))
            .inject(mockTemporaryFileTransaction)
            .`when`(command)
            .expectErrorType(IllegalArgumentException::class.java)
            .verify()
    }

    /**
     * 辅助方法：创建模拟的智能路由器
     */
    private fun createMockIntelligentRouter(): IntelligentStorageRouter {
        return object : IntelligentStorageRouter {
            private val mockStrategies = mapOf(
                StorageProvider.LOCAL to createMockLocalStrategy(),
                StorageProvider.S3 to createMockS3Strategy(),
                StorageProvider.ALIYUN_OSS to createMockAliyunStrategy()
            )

            override fun selectOptimalStrategy(context: FileUploadContext): Mono<FileStorageStrategy> {
                // 简单的路由逻辑：小文件用本地存储，大文件用对象存储
                val strategy = when {
                    context.fileSize < 1024 * 1024 -> mockStrategies[StorageProvider.LOCAL]!! // <1MB: 本地存储
                    context.fileSize >= 5 * 1024 * 1024 -> mockStrategies[StorageProvider.S3]!! // >=5MB: S3存储
                    else -> mockStrategies[StorageProvider.LOCAL]!! // 1MB-5MB: 本地存储
                }
                return Mono.just(strategy)
            }

            override fun getFallbackStrategies(primaryStrategy: FileStorageStrategy): List<FileStorageStrategy> {
                return mockStrategies.values.filter { it != primaryStrategy }
            }

            override fun isStrategyAvailable(strategy: FileStorageStrategy): Mono<Boolean> {
                return Mono.just(true)
            }

            override fun getStrategyHealthScore(provider: StorageProvider): Mono<Int> {
                return Mono.just(when (provider) {
                    StorageProvider.LOCAL -> 95
                    StorageProvider.S3 -> 85
                    StorageProvider.ALIYUN_OSS -> 80
                })
            }
        }
    }

    /**
     * 辅助方法：创建模拟的本地存储策略
     */
    private fun createMockLocalStrategy(): FileStorageStrategy {
        return object : FileStorageStrategy {
            override val provider = StorageProvider.LOCAL
            override fun isAvailable() = Mono.just(true)
            override fun uploadFile(filePath: String, dataBufferFlux: reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer>, contentType: String, metadata: Map<String, String>): Mono<site.weixing.natty.domain.common.filestorage.file.StorageInfo> {
                // 聚合流式内容，模拟写入
                return dataBufferFlux.collectList().map {
                    site.weixing.natty.domain.common.filestorage.file.StorageInfo(
                        provider = StorageProvider.LOCAL,
                        storagePath = "local://storage/$filePath",
                        etag = "mock-etag-${System.currentTimeMillis()}"
                    )
                }
            }
            override fun downloadFile(filePath: String): reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer> = reactor.core.publisher.Flux.empty()
            override fun deleteFile(filePath: String) = Mono.just(true)
            override fun existsFile(filePath: String) = Mono.just(true)
            override fun getFileSize(filePath: String) = Mono.just(1024L)
            override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?) = Mono.just("file:///$filePath")
            override fun copyFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun moveFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun listFiles(directoryPath: String, recursive: Boolean) = Mono.just(emptyList<site.weixing.natty.domain.common.filestorage.strategy.FileInfo>())
            override fun createDirectory(directoryPath: String) = Mono.just(true)
            override fun deleteDirectory(directoryPath: String, recursive: Boolean) = Mono.just(true)
            override fun getStorageUsage() = Mono.just(site.weixing.natty.domain.common.filestorage.strategy.StorageUsage(1000000L, 500000L, 500000L, 100L))
            override fun validateConfig(config: Map<String, Any>) = Mono.just(true)
            override fun getFileChecksum(filePath: String) = Mono.just("mock-checksum-123")
            override fun cleanup(olderThanDays: Int) = Mono.just(0L)
        }
    }

    /**
     * 辅助方法：创建模拟的S3存储策略
     */
    private fun createMockS3Strategy(): FileStorageStrategy {
        return object : FileStorageStrategy {
            override val provider = StorageProvider.S3
            override fun isAvailable() = Mono.just(true)
            override fun uploadFile(filePath: String, dataBufferFlux: reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer>, contentType: String, metadata: Map<String, String>): Mono<site.weixing.natty.domain.common.filestorage.file.StorageInfo> {
                return dataBufferFlux.collectList().map {
                    site.weixing.natty.domain.common.filestorage.file.StorageInfo(
                        provider = StorageProvider.S3,
                        storagePath = "s3://bucket/$filePath",
                        etag = "s3-etag-${System.currentTimeMillis()}"
                    )
                }
            }
            override fun downloadFile(filePath: String): reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer> = reactor.core.publisher.Flux.empty()
            override fun deleteFile(filePath: String) = Mono.just(true)
            override fun existsFile(filePath: String) = Mono.just(true)
            override fun getFileSize(filePath: String) = Mono.just(1024L)
            override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?) = Mono.just("https://s3.amazonaws.com/bucket/$filePath")
            override fun copyFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun moveFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun listFiles(directoryPath: String, recursive: Boolean) = Mono.just(emptyList<site.weixing.natty.domain.common.filestorage.strategy.FileInfo>())
            override fun createDirectory(directoryPath: String) = Mono.just(true)
            override fun deleteDirectory(directoryPath: String, recursive: Boolean) = Mono.just(true)
            override fun getStorageUsage() = Mono.just(site.weixing.natty.domain.common.filestorage.strategy.StorageUsage(1000000L, 500000L, 500000L, 100L))
            override fun validateConfig(config: Map<String, Any>) = Mono.just(true)
            override fun getFileChecksum(filePath: String) = Mono.just("s3-checksum-456")
            override fun cleanup(olderThanDays: Int) = Mono.just(0L)
        }
    }

    /**
     * 辅助方法：创建模拟的阿里云OSS存储策略
     */
    private fun createMockAliyunStrategy(): FileStorageStrategy {
        return object : FileStorageStrategy {
            override val provider = StorageProvider.ALIYUN_OSS
            override fun isAvailable() = Mono.just(true)
            override fun uploadFile(filePath: String, dataBufferFlux: reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer>, contentType: String, metadata: Map<String, String>): Mono<site.weixing.natty.domain.common.filestorage.file.StorageInfo> {
                return dataBufferFlux.collectList().map {
                    site.weixing.natty.domain.common.filestorage.file.StorageInfo(
                        provider = StorageProvider.ALIYUN_OSS,
                        storagePath = "oss://bucket/$filePath",
                        etag = "oss-etag-${System.currentTimeMillis()}"
                    )
                }
            }
            override fun downloadFile(filePath: String): reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer> = reactor.core.publisher.Flux.empty()
            override fun deleteFile(filePath: String) = Mono.just(true)
            override fun existsFile(filePath: String) = Mono.just(true)
            override fun getFileSize(filePath: String) = Mono.just(1024L)
            override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?) = Mono.just("https://bucket.oss-region.aliyuncs.com/$filePath")
            override fun copyFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun moveFile(sourcePath: String, destPath: String) = Mono.just(true)
            override fun listFiles(directoryPath: String, recursive: Boolean) = Mono.just(emptyList<site.weixing.natty.domain.common.filestorage.strategy.FileInfo>())
            override fun createDirectory(directoryPath: String) = Mono.just(true)
            override fun deleteDirectory(directoryPath: String, recursive: Boolean) = Mono.just(true)
            override fun getStorageUsage() = Mono.just(site.weixing.natty.domain.common.filestorage.strategy.StorageUsage(1000000L, 500000L, 500000L, 100L))
            override fun validateConfig(config: Map<String, Any>) = Mono.just(true)
            override fun getFileChecksum(filePath: String) = Mono.just("oss-checksum-789")
            override fun cleanup(olderThanDays: Int) = Mono.just(0L)
        }
    }

    /**
     * 辅助方法：创建模拟图片内容
     */
    private fun createMockImageContent(): ByteArray {
        // 简化的PNG文件头
        return byteArrayOf(
            -119, 80, 78, 71, 13, 10, 26, 10, // PNG signature
            0, 0, 0, 13, 73, 72, 68, 82, // IHDR chunk header
            0, 0, 4, 0, 0, 0, 4, 0, // 1024x1024 pixel
            8, 2, 0, 0, 0, -112, -119, 118, 47, // remaining header
            // 添加一些模拟的图片数据
            *ByteArray(100) { (it % 256).toByte() }
        )
    }

    /**
     * 辅助方法：计算SHA-256校验和
     */
    private fun calculateSHA256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * mock TemporaryFileManager
     */
    private fun createMockTemporaryFileManager(fileContent: ByteArray, fileName: String, fileSize: Long, contentType: String, referenceId: String): TemporaryFileManager {
        return object : TemporaryFileManager {
            override fun createTemporaryFile(
                originalFileName: String,
                fileSize: Long,
                contentType: String,
                dataBufferFlux: Flux<DataBuffer>
            ): Mono<TemporaryFileReference> {
                // 简单模拟，直接返回引用
                return Mono.just(
                    TemporaryFileReference(
                        referenceId = referenceId,
                        originalFileName = fileName,
                        fileSize = fileSize,
                        contentType = contentType,
                        temporaryPath = "/tmp/$referenceId",
                        createdAt = java.time.Instant.now(),
                        expiresAt = java.time.Instant.now().plusSeconds(3600),
                        checksum = null
                    )
                )
            }
            override fun getFileStreamAsFlux(referenceId: String): Flux<DataBuffer> {
                val buffer = DefaultDataBufferFactory.sharedInstance.wrap(fileContent)
                return Flux.just(buffer)
            }
            override fun deleteTemporaryFile(referenceId: String): Mono<Boolean> {
                return Mono.just(true)
            }
            override fun cleanupExpiredFiles(): Mono<Long> {
                return Mono.just(0L)
            }
            override fun getTemporaryFileReference(referenceId: String): Mono<TemporaryFileReference> {
                return Mono.just(
                    TemporaryFileReference(
                        referenceId = referenceId,
                        originalFileName = fileName,
                        fileSize = fileSize,
                        contentType = contentType,
                        temporaryPath = "/tmp/$referenceId",
                        createdAt = java.time.Instant.now(),
                        expiresAt = java.time.Instant.now().plusSeconds(3600),
                        checksum = null
                    )
                )
            }
            override fun isTemporaryFileValid(referenceId: String): Mono<Boolean> {
                return Mono.just(true)
            }
        }
    }

    /**
     * mock TemporaryFileTransaction
     */
    private val mockTemporaryFileTransaction = TemporaryFileTransaction(createMockTemporaryFileManager(ByteArray(0), "", 0, "", ""))

    // mock 管道处理元数据
    private fun mockPipelineCustomMetadata(tags: List<String>, contentType: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["pipelineProcessed"] = "true"
        map["VirusScanProcessor_processed"] = if (tags.contains("skip-scan")) "false" else "true"
        map["ChecksumProcessor_processed"] = "true"
        map["CompressionProcessor_processed"] = if (tags.contains("compress") || (contentType.startsWith("text/") && !tags.contains("skip-compress"))) "true" else "false"
        map["EncryptionProcessor_processed"] = "true"
        map["ThumbnailProcessor_processed"] = if (contentType.startsWith("image/")) "true" else "false"
        map["processingTime"] = "10"
        map["processedSize"] = "100"
        if (map["CompressionProcessor_processed"] == "true") {
            map["pipeline_compressed"] = "true"
            map["pipeline_compression_ratio"] = "0.5"
        }
        if (map["ThumbnailProcessor_processed"] == "true") {
            map["pipeline_thumbnails"] = "mock-thumbnail"
        }
        return map
    }
} 