package site.weixing.natty.server.common.filestorage

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.StandardCharsets

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadIntegrationTest(@Autowired val webTestClient: WebTestClient) {
    @Test
    fun `test traditional file upload`() {
        val content = "integration test file".toByteArray(StandardCharsets.UTF_8)
        val request = mapOf(
            "fileName" to "integration.txt",
            "folderId" to "test-folder",
            "uploaderId" to "test-user",
            "fileSize" to content.size,
            "contentType" to "text/plain",
            "fileContent" to content,
            "isPublic" to true
        )
        webTestClient.post().uri("/api/files/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.fileId").isNotEmpty
            .jsonPath("$.fileName").isEqualTo("integration.txt")
            .jsonPath("$.fileSize").isEqualTo(content.size)
    }
} 