package site.weixing.natty.domain.common.filestorage.file

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.FileStatus
import site.weixing.natty.domain.common.filestorage.service.TempFileStorageService
import java.security.MessageDigest

/**
 * 文件上传单元测试
 * 测试用户通过REST API上传文件到本地存储的完整流程
 */
class FileUploadTest {

    /**
     * 测试场景：用户通过REST上传文件到本地存储
     * 
     * 执行流程：
     * 1. 用户发送POST请求到 /file/upload
     * 2. UploadFile命令通过CommandGateway传递到File聚合根
     * 3. File.onUpload()处理命令，进行业务规则校验
     * 4. 生成FileUploaded事件
     * 5. FileState.onFileUploaded()更新聚合状态
     */
    @Test
    fun `should upload file successfully when valid upload command is received`() {
        // 准备测试数据
        val fileContent = "Hello, World! This is a test file content.".toByteArray()
        val expectedChecksum = calculateSHA256(fileContent)
        
        val command = UploadFile(
            fileName = "test-document.txt",
            folderId = "folder-123",
            uploaderId = "user-456",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            fileContent = fileContent,
            checksum = null, // 让系统自动计算校验和
            isPublic = false,
            tags = listOf("test", "document"),
            customMetadata = mapOf("category" to "测试文件", "priority" to "low"),
            replaceIfExists = false
        )

        // 执行测试并验证结果
        aggregateVerifier<File, FileState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                // 验证事件内容
                assertThat(event.fileName).isEqualTo(command.fileName)
                assertThat(event.folderId).isEqualTo(command.folderId)
                assertThat(event.uploaderId).isEqualTo(command.uploaderId)
                assertThat(event.fileSize).isEqualTo(command.fileSize)
                assertThat(event.contentType).isEqualTo(command.contentType)
                assertThat(event.checksum).isEqualTo(expectedChecksum)
                assertThat(event.isPublic).isEqualTo(command.isPublic)
                assertThat(event.tags).isEqualTo(command.tags)
                assertThat(event.customMetadata).isEqualTo(command.customMetadata)
                
                // 验证存储路径格式
                assertThat(event.storagePath).startsWith("folders/${command.folderId}/")
                assertThat(event.storagePath).endsWith("_${command.fileName}")
                
                // 验证临时文件ID
                assertThat(event.tempFileId).isNotBlank()
            }
            .expectState { state ->
                // 验证聚合状态
                assertThat(state.fileName).isEqualTo(command.fileName)
                assertThat(state.folderId).isEqualTo(command.folderId)
                assertThat(state.uploaderId).isEqualTo(command.uploaderId)
                assertThat(state.size).isEqualTo(command.fileSize)
                assertThat(state.contentType).isEqualTo(command.contentType)
                assertThat(state.status).isEqualTo(FileStatus.ACTIVE)
                assertThat(state.isPublic).isEqualTo(command.isPublic)
                assertThat(state.tags).isEqualTo(command.tags)
                assertThat(state.customMetadata).isEqualTo(command.customMetadata)
                
                // 验证存储信息
                assertThat(state.storageInfo).isNotNull
                assertThat(state.storageInfo!!.provider.name).isEqualTo("LOCAL")
                assertThat(state.storageInfo!!.etag).isEqualTo(expectedChecksum)
                
                // 验证版本管理
                assertThat(state.versions).hasSize(1)
                val initialVersion = state.versions.first()
                assertThat(initialVersion.version).isEqualTo(1)
                assertThat(initialVersion.size).isEqualTo(command.fileSize)
                assertThat(initialVersion.checksum).isEqualTo(expectedChecksum)
                assertThat(initialVersion.uploaderId).isEqualTo(command.uploaderId)
                
                // 验证时间戳
                assertThat(state.createdAt).isNotNull
                assertThat(state.updatedAt).isNotNull
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
            fileContent = fileContent,
            checksum = null,
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
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
            fileContent = fileContent,
            checksum = "invalid-checksum", // 错误的校验和
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .`when`(command)
            .expectErrorType(IllegalArgumentException::class.java)
            .verify()
    }

    /**
     * 测试场景：非法文件名
     */
    @Test
    fun `should throw exception when filename contains invalid characters`() {
        val fileContent = "Hello, World!".toByteArray()
        
        val command = UploadFile(
            fileName = "test/file<>.txt", // 包含非法字符
            folderId = "folder-123",
            uploaderId = "user-456",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            fileContent = fileContent,
            checksum = null,
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .`when`(command)
            .expectErrorType(IllegalArgumentException::class.java)
            .verify()
    }

    /**
     * 测试场景：包含自定义校验和的成功上传
     */
    @Test
    fun `should upload file successfully when valid checksum is provided`() {
        val fileContent = "Custom checksum test content".toByteArray()
        val expectedChecksum = calculateSHA256(fileContent)
        
        val command = UploadFile(
            fileName = "checksum-test.txt",
            folderId = "folder-789",
            uploaderId = "user-123",
            fileSize = fileContent.size.toLong(),
            contentType = "text/plain",
            fileContent = fileContent,
            checksum = expectedChecksum, // 提供正确的校验和
            isPublic = true,
            tags = listOf("checksum", "validation"),
            customMetadata = mapOf("source" to "unit-test"),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                assertThat(event.checksum).isEqualTo(expectedChecksum)
                assertThat(event.isPublic).isTrue
            }
            .expectState { state ->
                assertThat(state.storageInfo!!.etag).isEqualTo(expectedChecksum)
                assertThat(state.isPublic).isTrue
                assertThat(state.tags).contains("checksum", "validation")
                assertThat(state.customMetadata["source"]).isEqualTo("unit-test")
            }
            .verify()
    }

    /**
     * 测试场景：上传图片文件
     */
    @Test
    fun `should upload image file successfully with metadata`() {
        // 模拟图片文件内容（简化的二进制数据）
        val imageContent = byteArrayOf(
            -119, 80, 78, 71, 13, 10, 26, 10, // PNG signature
            0, 0, 0, 13, 73, 72, 68, 82, // IHDR chunk header
            0, 0, 0, 1, 0, 0, 0, 1, // 1x1 pixel
            8, 2, 0, 0, 0, -112, -119, 118, 47 // remaining header
        )
        
        val command = UploadFile(
            fileName = "profile-avatar.png",
            folderId = "avatars",
            uploaderId = "user-999",
            fileSize = imageContent.size.toLong(),
            contentType = "image/png",
            fileContent = imageContent,
            checksum = null,
            isPublic = true,
            tags = listOf("avatar", "profile", "image"),
            customMetadata = mapOf(
                "width" to "100",
                "height" to "100",
                "originalName" to "my-avatar.png"
            ),
            replaceIfExists = false
        )

        aggregateVerifier<File, FileState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(FileUploaded::class.java)
            .expectEventBody<FileUploaded> { event ->
                assertThat(event.fileName).isEqualTo("profile-avatar.png")
                assertThat(event.contentType).isEqualTo("image/png")
                assertThat(event.isPublic).isTrue
                assertThat(event.tags).containsExactly("avatar", "profile", "image")
                
                // 验证存储路径包含文件夹信息
                assertThat(event.storagePath).startsWith("folders/avatars/")
                assertThat(event.storagePath).endsWith("_profile-avatar.png")
            }
            .expectState { state ->
                assertThat(state.contentType).isEqualTo("image/png")
                assertThat(state.customMetadata["width"]).isEqualTo("100")
                assertThat(state.customMetadata["height"]).isEqualTo("100")
                assertThat(state.customMetadata["originalName"]).isEqualTo("my-avatar.png")
            }
            .verify()
    }

    /**
     * 辅助方法：计算SHA-256校验和
     */
    private fun calculateSHA256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
} 