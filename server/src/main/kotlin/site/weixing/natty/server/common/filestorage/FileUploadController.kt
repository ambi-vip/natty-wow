package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono
import java.security.MessageDigest

/**
 * 文件上传REST控制器
 * 提供文件上传的HTTP API端点
 */
@RestController
@RequestMapping("/api/files")
class FileUploadController(
    private val fileUploadApplicationService: FileUploadApplicationService
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 上传文件
     * POST /api/files/upload
     */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam("isPublic", defaultValue = "false") isPublic: Boolean,
        @RequestParam("tags", required = false) tags: List<String>?,
        @RequestParam("replaceIfExists", defaultValue = "false") replaceIfExists: Boolean,
        @RequestParam("customMetadata", required = false) customMetadataJson: String?
    ): Mono<ResponseEntity<FileUploadResponse>> {
        
        logger.info { "接收文件上传请求: ${file.originalFilename} (大小: ${file.size} 字节)" }
        
        return Mono.fromCallable {
            // 验证文件
            require(!file.isEmpty) { "文件不能为空" }
            require(file.originalFilename?.isNotBlank() == true) { "文件名不能为空" }
            require(file.size > 0) { "文件大小必须大于0" }
            
            val fileName = file.originalFilename!!
            val fileContent = file.bytes
            val contentType = file.contentType ?: "application/octet-stream"
            
            // 计算文件校验和
            val checksum = calculateSHA256(fileContent)
            
            // 解析自定义元数据（简化实现）
            val customMetadata = parseCustomMetadata(customMetadataJson)
            
            FileUploadRequest(
                fileName = fileName,
                folderId = folderId,
                uploaderId = uploaderId,
                fileSize = file.size,
                contentType = contentType,
                fileContent = fileContent,
                checksum = checksum,
                isPublic = isPublic,
                tags = tags ?: emptyList(),
                customMetadata = customMetadata,
                replaceIfExists = replaceIfExists
            )
        }
        .flatMap { request ->
            fileUploadApplicationService.uploadFile(request)
        }
        .map { fileId ->
            val response = FileUploadResponse(
                fileId = fileId,
                fileName = file.originalFilename!!,
                fileSize = file.size,
                contentType = file.contentType ?: "application/octet-stream",
                message = "文件上传成功"
            )
            ResponseEntity.ok(response)
        }
        .onErrorResume { error ->
            logger.error(error) { "文件上传失败: ${file.originalFilename}" }
            val errorResponse = FileUploadResponse(
                fileId = "",
                fileName = file.originalFilename ?: "unknown",
                fileSize = file.size,
                contentType = file.contentType ?: "application/octet-stream",
                message = "文件上传失败: ${error.message}",
                error = true
            )
            Mono.just(ResponseEntity.badRequest().body(errorResponse))
        }
    }
    
    /**
     * 获取文件信息
     * GET /api/files/{fileId}
     */
    @GetMapping("/{fileId}")
    fun getFileInfo(@PathVariable fileId: String): Mono<ResponseEntity<Any>> {
        // TODO: 实现文件信息查询
        return Mono.just(ResponseEntity.ok(mapOf(
            "fileId" to fileId,
            "message" to "文件信息查询功能待实现"
        )))
    }
    
    /**
     * 下载文件
     * GET /api/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    fun downloadFile(@PathVariable fileId: String): Mono<ResponseEntity<Any>> {
        // TODO: 实现文件下载
        return Mono.just(ResponseEntity.ok(mapOf(
            "fileId" to fileId,
            "message" to "文件下载功能待实现"
        )))
    }
    
    /**
     * 计算SHA-256校验和
     */
    private fun calculateSHA256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 解析自定义元数据JSON
     */
    private fun parseCustomMetadata(json: String?): Map<String, String> {
        // 简化实现，生产环境应该使用JSON解析库
        return if (json.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                // 这里应该使用Jackson或其他JSON库
                mapOf("raw" to json)
            } catch (e: Exception) {
                logger.warn(e) { "解析自定义元数据失败: $json" }
                emptyMap()
            }
        }
    }
}

/**
 * 文件上传响应
 */
data class FileUploadResponse(
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val message: String,
    val error: Boolean = false
) 