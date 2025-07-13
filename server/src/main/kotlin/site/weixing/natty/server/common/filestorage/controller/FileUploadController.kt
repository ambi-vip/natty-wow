package site.weixing.natty.server.common.filestorage.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import site.weixing.natty.server.common.filestorage.FileUploadApplicationService
import site.weixing.natty.server.common.filestorage.FileUploadRequest
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions

/**
 * 简化的文件上传控制器
 * 
 * 统一接口，支持：
 * 1. 基础文件上传
 * 2. 带处理选项的上传（压缩、加密、缩略图）
 * 3. 流式上传优化
 */
@RestController
@RequestMapping("/files")
class FileUploadController(
    private val fileUploadApplicationService: FileUploadApplicationService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(FileUploadController::class.java)
    }


    /**
     * 基础文件上传接口
     * 支持基本的文件上传功能
     */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("file") file: FilePart,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList()
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.info("收到文件上传请求: ${file.filename()}")
        
        val uploadRequest = FileUploadRequest(
            fileName = file.filename() ?: "unknown",
            folderId = folderId,
            uploaderId = uploaderId,
            fileSize = 1L, // WebFlux中无法预先获得大小
            contentType = "application/octet-stream",
            content = file.content(),
            isPublic = isPublic,
            tags = tags,
            customMetadata = mapOf(
                "originalFilename" to (file.filename() ?: "unknown"),
                "uploadVia" to "basic"
            ),
            processingOptions = ProcessingOptions() // 默认不处理
        )
        
        return fileUploadApplicationService.uploadFile(uploadRequest)
            .map { response -> ResponseEntity.ok(response) }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    FileUploadResponse(
                        fileId = null,
                        fileName = file.filename(),
                        fileSize = -1L,
                        uploadMethod = "basic",
                        message = "文件上传失败"
                    )
                )
            )
    }
    
    /**
     * 带处理选项的文件上传接口
     * 支持可选的压缩、加密、缩略图生成
     */
    @PostMapping("/upload/enhanced", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadWithProcessing(
        @RequestPart("file") file: FilePart,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList(),
        @RequestParam(value = "enableCompression", required = false) enableCompression: Boolean = false,
        @RequestParam(value = "requireEncryption", required = false) requireEncryption: Boolean = false,
        @RequestParam(value = "generateThumbnail", required = false) generateThumbnail: Boolean = false
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.info("收到增强上传请求: ${file.filename()}, 压缩:$enableCompression, 加密:$requireEncryption, 缩略图:$generateThumbnail")
        
        val processingOptions = ProcessingOptions(
            requireEncryption = requireEncryption,
            enableCompression = enableCompression,
            generateThumbnail = generateThumbnail
        )
        
        val uploadRequest = FileUploadRequest(
            fileName = file.filename() ?: "unknown",
            folderId = folderId,
            uploaderId = uploaderId,
            fileSize = 0L,
            contentType = "application/octet-stream",
            content = file.content(),
            isPublic = isPublic,
            tags = tags,
            customMetadata = mapOf(
                "originalFilename" to (file.filename() ?: "unknown"),
                "uploadVia" to "enhanced",
                "processingRequested" to "true"
            ),
            processingOptions = processingOptions
        )
        
        return fileUploadApplicationService.uploadFile(uploadRequest)
            .map { response -> ResponseEntity.ok(response) }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    FileUploadResponse(
                        fileId = null,
                        fileName = file.filename(),
                        fileSize = -1L,
                        uploadMethod = "enhanced",
                        message = "增强上传失败"
                    )
                )
            )
    }
    
}

/**
 * 文件上传响应
 */
data class FileUploadResponse(
    val fileId: String?,
    val fileName: String?,
    val fileSize: Long,
    val uploadMethod: String,
    val message: String,
    val checksum: String? = null,
    val storagePath: String? = null,
    val processingRequired: Boolean = false
) 