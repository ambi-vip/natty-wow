package site.weixing.natty.server.common.filestorage
//
//import me.ahoo.wow.command.CommandGateway
//import me.ahoo.wow.command.CommandResult
//import me.ahoo.wow.test.SagaVerifier
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.springframework.core.io.buffer.DefaultDataBufferFactory
//import org.springframework.core.io.buffer.DataBuffer
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import reactor.test.StepVerifier
//import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
//import site.weixing.natty.api.common.filestorage.file.UploadFile
//
///**
// * 文件上传应用服务测试
// * 专注于测试应用服务的协调逻辑
// */
//class FileUploadApplicationServiceTest {
//
//    private val mockCommandGateway = spyk<CommandGateway>(SagaVerifier.defaultCommandGateway())
//    private val applicationService = FileUploadApplicationService(mockCommandGateway)
//    private val dataBufferFactory = DefaultDataBufferFactory.sharedInstance
//
//    /**
//     * 测试成功的文件上传流程
//     */
//    @Test
//    fun `should upload file successfully`() {
//        // 准备测试数据
//        val fileContent = "Test file content"
//        val uploadRequest = FileUploadRequest(
//            fileName = "test.txt",
//            folderId = "folder-123",
//            uploaderId = "user-456",
//            fileSize = fileContent.length.toLong(),
//            contentType = "text/plain",
//            content = createDataBufferFlux(fileContent.toByteArray()),
//            isPublic = false,
//            tags = listOf("test"),
//            customMetadata = mapOf("category" to "document"),
//            processingOptions = ProcessingOptions()
//        )
//
//        // 模拟命令网关响应
//        val mockCommandResult = mockk<CommandResult.CommandSnapshot>()
//        every { mockCommandResult.aggregateId } returns "file-id-123"
//        every { mockCommandResult.result } returns mapOf(
//            "actualStoragePath" to "storage/test.txt",
//            "checksum" to "mock-checksum"
//        )
//
//        every {
//            mockCommandGateway.sendAndWaitForSnapshot(any<CommandMessage<UploadFile>>())
//        } returns Mono.just(mockCommandResult)
//
//        // 执行测试
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .assertNext { response ->
//                assertThat(response.fileId).isEqualTo("file-id-123")
//                assertThat(response.fileName).isEqualTo("storage/test.txt")
//                assertThat(response.fileSize).isEqualTo(fileContent.length.toLong())
//                assertThat(response.uploadMethod).isEqualTo("stream")
//                assertThat(response.message).isEqualTo("文件上传成功")
//                assertThat(response.checksum).isEqualTo("mock-checksum")
//                assertThat(response.storagePath).isEqualTo("storage/test.txt")
//                assertThat(response.processingRequired).isFalse()
//            }
//            .verifyComplete()
//
//        // 验证命令网关被正确调用
//        verify(exactly = 1) {
//            mockCommandGateway.sendAndWaitForSnapshot(any<CommandMessage<UploadFile>>())
//        }
//    }
//
//    /**
//     * 测试带处理选项的文件上传
//     */
//    @Test
//    fun `should upload file with processing options`() {
//        val fileContent = "Sensitive content"
//        val uploadRequest = FileUploadRequest(
//            fileName = "secret.txt",
//            folderId = "secure",
//            uploaderId = "user-789",
//            fileSize = fileContent.length.toLong(),
//            contentType = "text/plain",
//            content = createDataBufferFlux(fileContent.toByteArray()),
//            processingOptions = ProcessingOptions(
//                requireEncryption = true,
//                enableCompression = true
//            )
//        )
//
//        val mockCommandResult = mockk<CommandResult.CommandSnapshot>()
//        every { mockCommandResult.aggregateId } returns "file-id-456"
//        every { mockCommandResult.result } returns mapOf(
//            "actualStoragePath" to "storage/secret.txt",
//            "checksum" to "secure-checksum"
//        )
//
//        every {
//            mockCommandGateway.sendAndWaitForSnapshot(any<CommandMessage<UploadFile>>())
//        } returns Mono.just(mockCommandResult)
//
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .assertNext { response ->
//                assertThat(response.processingRequired).isTrue()
//                assertThat(response.fileId).isEqualTo("file-id-456")
//            }
//            .verifyComplete()
//    }
//
//    /**
//     * 测试文件上传验证失败
//     */
//    @Test
//    fun `should reject upload with invalid request`() {
//        val uploadRequest = FileUploadRequest(
//            fileName = "", // 空文件名
//            folderId = "folder-123",
//            uploaderId = "user-456",
//            fileSize = 100L,
//            contentType = "text/plain",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//    }
//
//    /**
//     * 测试文件大小限制
//     */
//    @Test
//    fun `should reject files exceeding size limit`() {
//        val tooLargeSize = 600 * 1024 * 1024L // 600MB，超过500MB限制
//
//        val uploadRequest = FileUploadRequest(
//            fileName = "huge-file.bin",
//            folderId = "folder-123",
//            uploaderId = "user-456",
//            fileSize = tooLargeSize,
//            contentType = "application/octet-stream",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//    }
//
//    /**
//     * 测试命令网关异常处理
//     */
//    @Test
//    fun `should handle command gateway errors`() {
//        val uploadRequest = FileUploadRequest(
//            fileName = "test.txt",
//            folderId = "folder-123",
//            uploaderId = "user-456",
//            fileSize = 100L,
//            contentType = "text/plain",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        every {
//            mockCommandGateway.sendAndWaitForSnapshot(any<CommandMessage<UploadFile>>())
//        } returns Mono.error(RuntimeException("Command execution failed"))
//
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .expectError(RuntimeException::class.java)
//            .verify()
//    }
//
//    /**
//     * 测试下载文件功能
//     */
//    @Test
//    fun `should create download request`() {
//        val downloadRequest = FileDownloadRequest(
//            fileId = "file-123",
//            userId = "user-456"
//        )
//
//        StepVerifier.create(applicationService.downloadFile(downloadRequest))
//            .assertNext { result ->
//                assertThat(result.fileId).isEqualTo("file-123")
//                assertThat(result.downloadUrl).isEqualTo("/api/files/file-123/download")
//                assertThat(result.fileName).isEqualTo("unknown") // 简化实现
//                assertThat(result.contentType).isEqualTo("application/octet-stream")
//            }
//            .verifyComplete()
//    }
//
//    /**
//     * 测试删除文件功能
//     */
//    @Test
//    fun `should delete file`() {
//        val deleteRequest = FileDeleteRequest(
//            fileId = "file-123",
//            deletedBy = "user-456",
//            reason = "不再需要"
//        )
//
//        StepVerifier.create(applicationService.deleteFile(deleteRequest))
//            .verifyComplete()
//    }
//
//    /**
//     * 测试请求验证逻辑
//     */
//    @Test
//    fun `should validate all required fields`() {
//        // 测试缺少文件夹ID
//        val requestMissingFolderId = FileUploadRequest(
//            fileName = "test.txt",
//            folderId = "", // 空文件夹ID
//            uploaderId = "user-456",
//            fileSize = 100L,
//            contentType = "text/plain",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        StepVerifier.create(applicationService.uploadFile(requestMissingFolderId))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//
//        // 测试缺少上传者ID
//        val requestMissingUploaderId = FileUploadRequest(
//            fileName = "test.txt",
//            folderId = "folder-123",
//            uploaderId = "", // 空上传者ID
//            fileSize = 100L,
//            contentType = "text/plain",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        StepVerifier.create(applicationService.uploadFile(requestMissingUploaderId))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//
//        // 测试零文件大小
//        val requestZeroSize = FileUploadRequest(
//            fileName = "test.txt",
//            folderId = "folder-123",
//            uploaderId = "user-456",
//            fileSize = 0L, // 零大小
//            contentType = "text/plain",
//            content = createDataBufferFlux("content".toByteArray())
//        )
//
//        StepVerifier.create(applicationService.uploadFile(requestZeroSize))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//    }
//
//    /**
//     * 测试命令构建逻辑
//     */
//    @Test
//    fun `should build upload command correctly`() {
//        val uploadRequest = FileUploadRequest(
//            fileName = "document.pdf",
//            folderId = "documents",
//            uploaderId = "user-123",
//            fileSize = 1024L,
//            contentType = "application/pdf",
//            content = createDataBufferFlux("pdf content".toByteArray()),
//            isPublic = true,
//            tags = listOf("important", "document"),
//            customMetadata = mapOf(
//                "department" to "HR",
//                "confidential" to "false"
//            ),
//            processingOptions = ProcessingOptions(
//                requireEncryption = false,
//                enableCompression = true
//            ),
//            checksum = "pre-calculated-checksum"
//        )
//
//        val mockCommandResult = mockk<CommandResult.CommandSnapshot>()
//        every { mockCommandResult.aggregateId } returns "file-id-789"
//        every { mockCommandResult.result } returns mapOf(
//            "actualStoragePath" to "storage/document.pdf",
//            "checksum" to "final-checksum"
//        )
//
//        every {
//            mockCommandGateway.sendAndWaitForSnapshot(any<CommandMessage<UploadFile>>())
//        } returns Mono.just(mockCommandResult)
//
//        StepVerifier.create(applicationService.uploadFile(uploadRequest))
//            .assertNext { response ->
//                assertThat(response.fileId).isEqualTo("file-id-789")
//                assertThat(response.fileName).isEqualTo("storage/document.pdf")
//                assertThat(response.processingRequired).isTrue() // 压缩选项启用
//            }
//            .verifyComplete()
//
//        // 验证传递给命令网关的命令内容
//        verify(exactly = 1) {
//            mockCommandGateway.sendAndWaitForSnapshot(match<CommandMessage<UploadFile>> { commandMessage ->
//                val command = commandMessage.command
//                command.fileName == "document.pdf" &&
//                command.folderId == "documents" &&
//                command.uploaderId == "user-123" &&
//                command.isPublic == true &&
//                command.tags.contains("important") &&
//                command.tags.contains("document") &&
//                command.customMetadata["department"] == "HR" &&
//                command.customMetadata["uploadMethod"] == "stream" &&
//                command.processingOptions.enableCompression == true &&
//                command.checksum == "pre-calculated-checksum"
//            })
//        }
//    }
//
//    /**
//     * 创建数据缓冲区流
//     */
//    private fun createDataBufferFlux(content: ByteArray): Flux<DataBuffer> {
//        val buffer = dataBufferFactory.wrap(content)
//        return Flux.just(buffer)
//    }
//}