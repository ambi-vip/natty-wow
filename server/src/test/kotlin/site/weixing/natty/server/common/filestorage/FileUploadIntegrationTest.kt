package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import site.weixing.natty.domain.common.filestorage.service.TempFileStorageService
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Duration

/**
 * 文件上传集成测试
 * 测试从REST API到物理文件存储的完整流程
 */
@SpringBootTest
@ActiveProfiles("test")
class FileUploadIntegrationTest {

    @Autowired
    private lateinit var fileUploadApplicationService: FileUploadApplicationService
    
    @Autowired 
    private lateinit var tempFileStorageService: TempFileStorageService
    
    @Autowired
    private lateinit var fileStorageEventHandler: FileStorageEventHandler

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val TEST_BASE_DIR = "/tmp/natty-files-test"
    }

    @BeforeEach
    fun setUp() {
        // 清理测试目录
        val testDir = Paths.get(TEST_BASE_DIR)
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(testDir)
        
        logger.info { "测试环境已清理和初始化" }
    }

    /**
     * 测试完整的文件上传流程
     * 验证：REST请求 -> 应用服务 -> 命令处理 -> 事件发布 -> 事件处理 -> 物理存储
     */
    @Test
    fun `should complete full file upload workflow`() {
        // 准备测试数据
        val testContent = "这是一个测试文件的内容，用于验证文件上传功能。包含中文字符测试。".toByteArray()
        val testFileName = "test-file.txt"
        val testFolderId = "test-folder-001"
        val testUploaderId = "user-123"
        val expectedChecksum = calculateSHA256(testContent)
        
        val uploadRequest = FileUploadRequest(
            fileName = testFileName,
            folderId = testFolderId,
            uploaderId = testUploaderId,
            fileSize = testContent.size.toLong(),
            contentType = "text/plain",
            fileContent = testContent,
            checksum = expectedChecksum,
            isPublic = false,
            tags = listOf("test", "integration"),
            customMetadata = mapOf("source" to "integration-test"),
            replaceIfExists = false
        )

        // 执行文件上传
        StepVerifier.create(
            fileUploadApplicationService.uploadFile(uploadRequest)
        )
        .expectNextMatches { fileId ->
            // 验证返回的文件ID不为空
            assertNotNull(fileId)
            assertTrue(fileId.isNotBlank())
            
            logger.info { "文件上传成功，文件ID: $fileId" }
            
            // 验证临时文件存储
            val tempFileCount = tempFileStorageService.getTempFileCount()
            logger.info { "当前临时文件数量: $tempFileCount" }
            
            true
        }
        .verifyComplete()
        
        // 等待异步事件处理完成
        Thread.sleep(2000)
        
        // 验证物理文件是否已创建
        verifyPhysicalFileCreation(testFolderId, testFileName, testContent)
    }

    /**
     * 测试大文件上传
     */
    @Test
    fun `should handle large file upload`() {
        // 创建1MB的测试文件
        val largeContent = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val fileName = "large-test-file.bin"
        val folderId = "test-folder-002"
        val uploaderId = "user-456"
        
        val uploadRequest = FileUploadRequest(
            fileName = fileName,
            folderId = folderId,
            uploaderId = uploaderId,
            fileSize = largeContent.size.toLong(),
            contentType = "application/octet-stream",
            fileContent = largeContent,
            checksum = calculateSHA256(largeContent),
            isPublic = true,
            tags = listOf("large", "binary", "test"),
            customMetadata = mapOf(
                "size" to "1MB",
                "type" to "binary"
            ),
            replaceIfExists = false
        )

        StepVerifier.create(
            fileUploadApplicationService.uploadFile(uploadRequest)
        )
        .expectNextMatches { fileId ->
            assertNotNull(fileId)
            assertTrue(fileId.isNotBlank())
            logger.info { "大文件上传成功，文件ID: $fileId" }
            true
        }
        .verifyComplete()
        
        // 等待异步处理
        Thread.sleep(3000)
        
        // 验证大文件的物理存储
        verifyPhysicalFileCreation(folderId, fileName, largeContent)
    }

    /**
     * 测试文件上传失败场景
     */
    @Test
    fun `should handle upload failure gracefully`() {
        // 创建一个校验和不匹配的请求
        val testContent = "test content".toByteArray()
        val wrongChecksum = "wrong-checksum-value"
        
        val invalidRequest = FileUploadRequest(
            fileName = "invalid-file.txt",
            folderId = "test-folder-003",
            uploaderId = "user-789",
            fileSize = testContent.size.toLong(),
            contentType = "text/plain",
            fileContent = testContent,
            checksum = wrongChecksum, // 错误的校验和
            isPublic = false,
            tags = emptyList(),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        StepVerifier.create(
            fileUploadApplicationService.uploadFile(invalidRequest)
        )
        .expectError()
        .verify(Duration.ofSeconds(5))
        
        logger.info { "错误的校验和请求正确地被拒绝" }
    }

    /**
     * 测试临时文件清理
     */
    @Test
    fun `should cleanup temporary files after processing`() {
        val testContent = "临时文件清理测试".toByteArray()
        val fileName = "cleanup-test.txt"
        
        val uploadRequest = FileUploadRequest(
            fileName = fileName,
            folderId = "cleanup-folder",
            uploaderId = "user-cleanup",
            fileSize = testContent.size.toLong(),
            contentType = "text/plain",
            fileContent = testContent,
            checksum = calculateSHA256(testContent),
            isPublic = false,
            tags = listOf("cleanup-test"),
            customMetadata = emptyMap(),
            replaceIfExists = false
        )

        val initialTempFileCount = tempFileStorageService.getTempFileCount()
        
        StepVerifier.create(
            fileUploadApplicationService.uploadFile(uploadRequest)
        )
        .expectNextCount(1)
        .verifyComplete()
        
        // 等待事件处理和清理
        Thread.sleep(3000)
        
        // 验证临时文件已被清理
        val finalTempFileCount = tempFileStorageService.getTempFileCount()
        logger.info { "初始临时文件数: $initialTempFileCount, 最终临时文件数: $finalTempFileCount" }
        
        // 由于异步清理，临时文件数量应该没有显著增加
        assertTrue(finalTempFileCount <= initialTempFileCount + 1)
    }

    /**
     * 验证物理文件创建
     */
    private fun verifyPhysicalFileCreation(folderId: String, fileName: String, expectedContent: ByteArray) {
        // 根据存储路径生成规则构建预期的文件路径
        val expectedBasePath = Paths.get(TEST_BASE_DIR, "folders", folderId)
        
        // 查找匹配的文件（因为文件名包含时间戳）
        if (Files.exists(expectedBasePath)) {
            val matchingFiles = Files.list(expectedBasePath)
                .filter { it.fileName.toString().endsWith("_$fileName") }
                .toList()
            
            assertTrue(matchingFiles.isNotEmpty(), "应该找到匹配的物理文件")
            
            val physicalFile = matchingFiles.first()
            assertTrue(Files.exists(physicalFile), "物理文件应该存在: $physicalFile")
            
            // 验证文件内容
            val actualContent = Files.readAllBytes(physicalFile)
            assertArrayEquals(expectedContent, actualContent, "文件内容应该匹配")
            
            logger.info { "物理文件验证成功: $physicalFile (大小: ${actualContent.size} 字节)" }
        } else {
            fail("预期的基础路径不存在: $expectedBasePath")
        }
    }

    /**
     * 计算SHA-256校验和
     */
    private fun calculateSHA256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
} 