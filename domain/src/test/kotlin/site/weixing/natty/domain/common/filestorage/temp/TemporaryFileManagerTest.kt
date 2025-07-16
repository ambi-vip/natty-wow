package site.weixing.natty.domain.common.filestorage.temp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

class TemporaryFileManagerTest {
//    @Test
//    fun `test create and get temporary file reference`() {
//        val manager = TemporaryFileManager("/tmp/natty-test")
//        val content = "hello world".toByteArray()
//        val ref = manager.createTemporaryFile(
//            originalFileName = "test.txt",
//            fileSize = content.size.toLong(),
//            contentType = "text/plain",
//            inputStream = ByteArrayInputStream(content),
//            expiresAt = LocalDateTime.now().plusMinutes(10)
//        ).block()!!
//        assertEquals("test.txt", ref.originalFileName)
//        assertEquals(content.size.toLong(), ref.fileSize)
//        assertEquals("text/plain", ref.contentType)
//        assertFalse(ref.isExpired())
//        val loaded = manager.getTemporaryFileReference(ref.referenceId).block()
//        assertNotNull(loaded)
//        assertEquals(ref.referenceId, loaded?.referenceId)
//    }
//
//    @Test
//    fun `test delete temporary file`() {
//        val manager = TemporaryFileManager("/tmp/natty-test")
//        val content = "delete me".toByteArray()
//        val ref = manager.createTemporaryFile(
//            originalFileName = "delete.txt",
//            fileSize = content.size.toLong(),
//            contentType = "text/plain",
//            inputStream = ByteArrayInputStream(content),
//            expiresAt = LocalDateTime.now().plusMinutes(1)
//        ).block()!!
//        val deleted = manager.deleteTemporaryFile(ref.referenceId).block()
//        assertTrue(deleted)
//        val loaded = manager.getTemporaryFileReference(ref.referenceId).block()
//        assertNull(loaded)
//    }
} 