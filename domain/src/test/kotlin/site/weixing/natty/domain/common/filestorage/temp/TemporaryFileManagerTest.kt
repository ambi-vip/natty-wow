package site.weixing.natty.domain.common.filestorage.temp

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import site.weixing.natty.domain.common.filestorage.fixtures.*
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import java.time.Duration
import java.time.LocalDateTime

/**
 * TemporaryFileManager单元测试
 * 测试临时文件管理器的文件操作和生命周期管理
 */
@DisplayName("TemporaryFileManager Tests")
class TemporaryFileManagerTest : ReactiveTestBase() {

    private lateinit var tempFileStorage: FileStorageService
    private lateinit var tempFileManager: TemporaryFileManager

    @BeforeEach
    override fun setUp() {
        super.setUp()
        tempFileStorage = mockk()
        tempFileManager = mockk()
    }

//    @Test
//    @DisplayName("Should create temporary file successfully")
//    fun `should create temporary file successfully`() {
//        // Given
//        val fileName = "test-file.txt"
//        val fileContent = "Test file content".toByteArray()
//        val tempFileId = "temp-file-123"
//        val tempFile = TestDataFactory.createTemporaryFile(
//            id = tempFileId,
//            originalFileName = fileName,
//            size = fileContent.size.toLong()
//        )
//
//        // 获取 DefaultDataBufferFactory 实例
//        val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory()
//
//        // 将字符串转换为 DataBuffer，并创建 Flux<DataBuffer>
//        val dataBufferFlux: Flux<DataBuffer> = Flux.just(fileContent)
//            .map { byteArray ->
//                dataBufferFactory.wrap(byteArray) // 将字节数组包装为 DataBuffer
//            }
//
//        every { tempFileStorage.store(fileName, fileContent) } returns Mono.just(tempFile)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(fileName, fileContent.size.toLong(), "text", dataBufferFlux))
//            .expectNext(tempFileId)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.store(fileName, fileContent) }
//    }
//
//    @Test
//    @DisplayName("Should handle storage failure during file creation")
//    fun `should handle storage failure during file creation`() {
//        // Given
//        val fileName = "test-file.txt"
//        val fileContent = "Test file content".toByteArray()
//        val storageError = RuntimeException("Storage failed")
//
//        every { tempFileStorage.store(fileName, fileContent) } returns Mono.error(storageError)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(fileName, fileContent))
//            .expectError(RuntimeException::class.java)
//            .verify()
//
//        verify(exactly = 1) { tempFileStorage.store(fileName, fileContent) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", " ", "file/with/slash", "file\\with\\backslash"])
//    @DisplayName("Should reject invalid file names")
//    fun `should reject invalid file names`(invalidFileName: String) {
//        // Given
//        val fileContent = "Test content".toByteArray()
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(invalidFileName, fileContent))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//
//        verify(exactly = 0) { tempFileStorage.store(any(), any()) }
//    }
//
//    @Test
//    @DisplayName("Should reject empty file content")
//    fun `should reject empty file content`() {
//        // Given
//        val fileName = "test-file.txt"
//        val emptyContent = byteArrayOf()
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(fileName, emptyContent))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//
//        verify(exactly = 0) { tempFileStorage.store(any(), any()) }
//    }
//
//    @Test
//    @DisplayName("Should get temporary file successfully")
//    fun `should get temporary file successfully`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val tempFile = TestDataFactory.createTemporaryFile(
//            id = tempFileId,
//            originalFileName = "test-file.txt",
//            size = 1024L
//        )
//
//        every { tempFileStorage.get(tempFileId) } returns Mono.just(tempFile)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFile(tempFileId))
//            .expectNext(tempFile)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.get(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should handle file not found")
//    fun `should handle file not found`() {
//        // Given
//        val tempFileId = "non-existent-file"
//
//        every { tempFileStorage.get(tempFileId) } returns Mono.empty()
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFile(tempFileId))
//            .expectComplete()
//            .verify()
//
//        verify(exactly = 1) { tempFileStorage.get(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should validate existing temporary file")
//    fun `should validate existing temporary file`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val tempFile = TestDataFactory.createTemporaryFile(
//            id = tempFileId,
//            originalFileName = "test-file.txt",
//            size = 1024L,
//            expiresAt = LocalDateTime.now().plusHours(1) // Not expired
//        )
//
//        every { tempFileStorage.get(tempFileId) } returns Mono.just(tempFile)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.validateTemporaryFile(tempFileId))
//            .expectNext(true)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.get(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should invalidate expired temporary file")
//    fun `should invalidate expired temporary file`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val expiredTempFile = TestDataFactory.createTemporaryFile(
//            id = tempFileId,
//            originalFileName = "test-file.txt",
//            size = 1024L,
//            expiresAt = LocalDateTime.now().minusHours(1) // Expired
//        )
//
//        every { tempFileStorage.get(tempFileId) } returns Mono.just(expiredTempFile)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.validateTemporaryFile(tempFileId))
//            .expectNext(false)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.get(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should invalidate non-existent temporary file")
//    fun `should invalidate non-existent temporary file`() {
//        // Given
//        val tempFileId = "non-existent-file"
//
//        every { tempFileStorage.get(tempFileId) } returns Mono.empty()
//
//        // When & Then
//        StepVerifier.create(tempFileManager.validateTemporaryFile(tempFileId))
//            .expectNext(false)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.get(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should delete temporary file successfully")
//    fun `should delete temporary file successfully`() {
//        // Given
//        val tempFileId = "temp-file-123"
//
//        every { tempFileStorage.delete(tempFileId) } returns Mono.just(true)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.deleteTemporaryFile(tempFileId))
//            .expectNext(true)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.delete(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should handle deletion failure gracefully")
//    fun `should handle deletion failure gracefully`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val deletionError = RuntimeException("Deletion failed")
//
//        every { tempFileStorage.delete(tempFileId) } returns Mono.error(deletionError)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.deleteTemporaryFile(tempFileId))
//            .expectNext(false)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.delete(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should get temporary file content successfully")
//    fun `should get temporary file content successfully`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val fileContent = "Test file content".toByteArray()
//
//        every { tempFileStorage.getContent(tempFileId) } returns Mono.just(fileContent)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFileContent(tempFileId))
//            .expectNext(fileContent)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.getContent(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should handle content retrieval failure")
//    fun `should handle content retrieval failure`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val retrievalError = RuntimeException("Content retrieval failed")
//
//        every { tempFileStorage.getContent(tempFileId) } returns Mono.error(retrievalError)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFileContent(tempFileId))
//            .expectError(RuntimeException::class.java)
//            .verify()
//
//        verify(exactly = 1) { tempFileStorage.getContent(tempFileId) }
//    }
//
//    @Test
//    @DisplayName("Should cleanup expired files successfully")
//    fun `should cleanup expired files successfully`() {
//        // Given
//        val expiredFileIds = listOf("expired-1", "expired-2", "expired-3")
//
//        every { tempFileStorage.findExpiredFiles() } returns Mono.just(expiredFileIds)
//        expiredFileIds.forEach { fileId ->
//            every { tempFileStorage.delete(fileId) } returns Mono.just(true)
//        }
//
//        // When & Then
//        StepVerifier.create(tempFileManager.cleanupExpiredFiles())
//            .expectNext(3)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.findExpiredFiles() }
//        expiredFileIds.forEach { fileId ->
//            verify(exactly = 1) { tempFileStorage.delete(fileId) }
//        }
//    }
//
//    @Test
//    @DisplayName("Should handle partial cleanup failure")
//    fun `should handle partial cleanup failure`() {
//        // Given
//        val expiredFileIds = listOf("expired-1", "expired-2", "expired-3")
//
//        every { tempFileStorage.findExpiredFiles() } returns Mono.just(expiredFileIds)
//        every { tempFileStorage.delete("expired-1") } returns Mono.just(true)
//        every { tempFileStorage.delete("expired-2") } returns Mono.error(RuntimeException("Deletion failed"))
//        every { tempFileStorage.delete("expired-3") } returns Mono.just(true)
//
//        // When & Then - Should return count of successfully deleted files
//        StepVerifier.create(tempFileManager.cleanupExpiredFiles())
//            .expectNext(2)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.findExpiredFiles() }
//        expiredFileIds.forEach { fileId ->
//            verify(exactly = 1) { tempFileStorage.delete(fileId) }
//        }
//    }
//
//    @Test
//    @DisplayName("Should handle no expired files")
//    fun `should handle no expired files`() {
//        // Given
//        every { tempFileStorage.findExpiredFiles() } returns Mono.just(emptyList())
//
//        // When & Then
//        StepVerifier.create(tempFileManager.cleanupExpiredFiles())
//            .expectNext(0)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.findExpiredFiles() }
//        verify(exactly = 0) { tempFileStorage.delete(any()) }
//    }
//
//    @Test
//    @DisplayName("Should extend temporary file expiration")
//    fun `should extend temporary file expiration`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val extensionDuration = Duration.ofHours(2)
//        val newExpirationTime = LocalDateTime.now().plus(extensionDuration)
//
//        every { tempFileStorage.extendExpiration(tempFileId, extensionDuration) } returns Mono.just(newExpirationTime)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.extendTemporaryFileExpiration(tempFileId, extensionDuration))
//            .expectNext(newExpirationTime)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.extendExpiration(tempFileId, extensionDuration) }
//    }
//
//    @Test
//    @DisplayName("Should handle extension failure")
//    fun `should handle extension failure`() {
//        // Given
//        val tempFileId = "temp-file-123"
//        val extensionDuration = Duration.ofHours(2)
//        val extensionError = RuntimeException("Extension failed")
//
//        every { tempFileStorage.extendExpiration(tempFileId, extensionDuration) } returns Mono.error(extensionError)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.extendTemporaryFileExpiration(tempFileId, extensionDuration))
//            .expectError(RuntimeException::class.java)
//            .verify()
//
//        verify(exactly = 1) { tempFileStorage.extendExpiration(tempFileId, extensionDuration) }
//    }
//
//    @Test
//    @DisplayName("Should get temporary file statistics")
//    fun `should get temporary file statistics`() {
//        // Given
//        val stats = TestDataFactory.createTemporaryFileStats(
//            totalFiles = 150,
//            totalSize = 1024 * 1024 * 50L, // 50MB
//            expiredFiles = 25
//        )
//
//        every { tempFileStorage.getStatistics() } returns Mono.just(stats)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFileStatistics())
//            .expectNext(stats)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.getStatistics() }
//    }
//
//    @Test
//    @DisplayName("Should handle statistics retrieval failure")
//    fun `should handle statistics retrieval failure`() {
//        // Given
//        val statsError = RuntimeException("Statistics retrieval failed")
//
//        every { tempFileStorage.getStatistics() } returns Mono.error(statsError)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.getTemporaryFileStatistics())
//            .expectError(RuntimeException::class.java)
//            .verify()
//
//        verify(exactly = 1) { tempFileStorage.getStatistics() }
//    }
//
//    @Test
//    @DisplayName("Should list temporary files with pagination")
//    fun `should list temporary files with pagination`() {
//        // Given
//        val pageSize = 10
//        val pageNumber = 0
//        val tempFiles = (1..pageSize).map { index ->
//            TestDataFactory.createTemporaryFile(
//                id = "temp-file-$index",
//                originalFileName = "file-$index.txt",
//                size = 1024L * index
//            )
//        }
//
//        every { tempFileStorage.list(pageNumber, pageSize) } returns Mono.just(tempFiles)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.listTemporaryFiles(pageNumber, pageSize))
//            .expectNext(tempFiles)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.list(pageNumber, pageSize) }
//    }
//
//    @Test
//    @DisplayName("Should handle empty page result")
//    fun `should handle empty page result`() {
//        // Given
//        val pageSize = 10
//        val pageNumber = 5
//
//        every { tempFileStorage.list(pageNumber, pageSize) } returns Mono.just(emptyList())
//
//        // When & Then
//        StepVerifier.create(tempFileManager.listTemporaryFiles(pageNumber, pageSize))
//            .expectNext(emptyList())
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.list(pageNumber, pageSize) }
//    }
//
//    @Test
//    @DisplayName("Should validate file size limits")
//    fun `should validate file size limits`() {
//        // Given
//        val fileName = "large-file.txt"
//        val maxFileSize = 1024 * 1024L // 1MB
//        val oversizedContent = ByteArray((maxFileSize + 1).toInt()) { 0 }
//
//        tempFileManager.setMaxFileSize(maxFileSize)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(fileName, oversizedContent))
//            .expectError(IllegalArgumentException::class.java)
//            .verify()
//
//        verify(exactly = 0) { tempFileStorage.store(any(), any()) }
//    }
//
//    @Test
//    @DisplayName("Should accept files within size limits")
//    fun `should accept files within size limits`() {
//        // Given
//        val fileName = "normal-file.txt"
//        val maxFileSize = 1024 * 1024L // 1MB
//        val normalContent = ByteArray((maxFileSize / 2).toInt()) { 0 }
//        val tempFileId = "temp-file-123"
//        val tempFile = TestDataFactory.createTemporaryFile(
//            id = tempFileId,
//            originalFileName = fileName,
//            size = normalContent.size.toLong()
//        )
//
//        tempFileManager.setMaxFileSize(maxFileSize)
//        every { tempFileStorage.store(fileName, normalContent) } returns Mono.just(tempFile)
//
//        // When & Then
//        StepVerifier.create(tempFileManager.createTemporaryFile(fileName, normalContent))
//            .expectNext(tempFileId)
//            .verifyComplete()
//
//        verify(exactly = 1) { tempFileStorage.store(fileName, normalContent) }
//    }
//
//    @Test
//    @DisplayName("Should handle concurrent file operations")
//    fun `should handle concurrent file operations`() {
//        // Given
//        val fileCount = 10
//        val fileName = "concurrent-file.txt"
//        val fileContent = "Test content".toByteArray()
//
//        (1..fileCount).forEach { index ->
//            val tempFileId = "temp-file-$index"
//            val tempFile = TestDataFactory.createTemporaryFile(
//                id = tempFileId,
//                originalFileName = "$index-$fileName",
//                size = fileContent.size.toLong()
//            )
//            every { tempFileStorage.store("$index-$fileName", fileContent) } returns Mono.just(tempFile)
//        }
//
//        // When - Create multiple files concurrently
//        val operations = (1..fileCount).map { index ->
//            tempFileManager.createTemporaryFile("$index-$fileName", fileContent)
//        }
//
//        // Then
//        StepVerifier.create(Mono.zip(operations) { results -> results.toList() })
//            .assertNext { results ->
//                assertThat(results).hasSize(fileCount)
//                results.forEachIndexed { index, result ->
//                    assertThat(result).isEqualTo("temp-file-${index + 1}")
//                }
//            }
//            .verifyComplete()
//
//        // Verify all storage calls were made
//        (1..fileCount).forEach { index ->
//            verify(exactly = 1) { tempFileStorage.store("$index-$fileName", fileContent) }
//        }
//    }
}