package site.weixing.natty.server.common.filestorage.controller

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import site.weixing.natty.server.common.filestorage.FileUploadApplicationService
import site.weixing.natty.server.common.filestorage.FileUploadRequest
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
import site.weixing.natty.api.common.filestorage.file.FileUploadResponse
import org.springframework.http.HttpStatus
import site.weixing.natty.server.common.filestorage.FileUploadBusinessException
import site.weixing.natty.server.common.filestorage.FileUploadTechnicalException
import site.weixing.natty.server.common.filestorage.FileValidationException
import site.weixing.natty.server.common.filestorage.TemporaryFileException

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
        @RequestParam("bucketId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList(),
        @RequestHeader(value = "Content-Length", required = false) contentLength: Long?
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.info("收到文件上传请求: ${file.filename()}")

        val fileSize = contentLength ?: 0L

        return processUpload(
            file = file,
            folderId = folderId,
            uploaderId = uploaderId,
            isPublic = isPublic,
            tags = tags,
            processingOptions = ProcessingOptions(),
            uploadMethod = "basic",
            fileSize = fileSize
        )
    }

    /**
     * 带处理选项的文件上传接口
     * 支持可选的压缩、加密、缩略图生成
     */
    @PostMapping("/upload/enhanced", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadWithProcessing(
        @RequestPart("file") file: FilePart,
        @RequestParam("bucketId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList(),
        @RequestParam(value = "enableCompression", required = false) enableCompression: Boolean = false,
        @RequestParam(value = "requireEncryption", required = false) requireEncryption: Boolean = false,
        @RequestParam(value = "generateThumbnail", required = false) generateThumbnail: Boolean = false,
        @RequestHeader(value = "Content-Length", required = false) contentLength: Long?
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.info("收到增强上传请求: ${file.filename()}, 压缩:$enableCompression, 加密:$requireEncryption, 缩略图:$generateThumbnail")

        val processingOptions = ProcessingOptions(
            requireEncryption = requireEncryption,
            enableCompression = enableCompression,
            generateThumbnail = generateThumbnail
        )

        val fileSize = contentLength ?: 0L

        return processUpload(
            file = file,
            folderId = folderId,
            uploaderId = uploaderId,
            isPublic = isPublic,
            tags = tags,
            processingOptions = processingOptions,
            uploadMethod = "enhanced",
            fileSize = fileSize
        )
    }

    /**
     * 统一的文件上传处理方法
     * 消除重复代码，提高可维护性
     */
    private fun processUpload(
        file: FilePart,
        folderId: String,
        uploaderId: String,
        isPublic: Boolean,
        tags: List<String>,
        processingOptions: ProcessingOptions,
        uploadMethod: String,
        fileSize: Long
    ): Mono<ResponseEntity<FileUploadResponse>> {
        val uploadRequest = createUploadRequest(
            file, folderId, uploaderId, isPublic, tags, processingOptions, uploadMethod, fileSize
        )

        return fileUploadApplicationService.uploadFile(uploadRequest)
            .map { response -> ResponseEntity.ok(response) }
            .onErrorResume { error -> handleUploadError(error, file.filename(), uploadMethod) }
    }

    /**
     * 创建文件上传请求对象
     */
    private fun createUploadRequest(
        file: FilePart,
        folderId: String,
        uploaderId: String,
        isPublic: Boolean,
        tags: List<String>,
        processingOptions: ProcessingOptions,
        uploadMethod: String,
        fileSize: Long
    ): FileUploadRequest {
        val fileName = file.filename() ?: "unknown"
        val customMetadata = mutableMapOf(
            "originalFilename" to fileName,
            "uploadVia" to uploadMethod
        )
        
        if (processingOptions.requiresProcessing()) {
            customMetadata["processingRequested"] = "true"
        }

        return FileUploadRequest(
            fileName = fileName,
            bucketId = folderId,
            uploaderId = uploaderId,
            fileSize = if (fileSize > 0) fileSize else 0L,
            contentType = "application/octet-stream",
            content = file.content(),
            isPublic = isPublic,
            tags = tags,
            customMetadata = customMetadata,
            processingOptions = processingOptions
        )
    }

    /**
     * 处理上传错误，根据异常类型返回不同的HTTP状态码和错误信息
     */
    private fun handleUploadError(
        error: Throwable,
        fileName: String?,
        uploadMethod: String
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.error("文件上传失败: fileName=$fileName, uploadMethod=$uploadMethod", error)
        
        val (httpStatus, errorMessage) = when (error) {
            is FileValidationException -> {
                HttpStatus.BAD_REQUEST to "文件验证失败: ${error.message}"
            }
            is FileUploadBusinessException -> {
                HttpStatus.BAD_REQUEST to "业务规则验证失败: ${error.message}"
            }
            is TemporaryFileException -> {
                HttpStatus.INTERNAL_SERVER_ERROR to "临时文件处理失败"
            }
            is FileUploadTechnicalException -> {
                HttpStatus.INTERNAL_SERVER_ERROR to "系统异常，请稍后重试"
            }
            else -> {
                HttpStatus.INTERNAL_SERVER_ERROR to when (uploadMethod) {
                    "basic" -> "文件上传失败"
                    "enhanced" -> "增强上传失败"
                    else -> "文件上传失败"
                }
            }
        }

        val response = FileUploadResponse(
            fileId = null,
            fileName = fileName,
            fileSize = -1L,
            uploadMethod = uploadMethod,
            message = errorMessage
        )

        return Mono.just(ResponseEntity.status(httpStatus).body(response))
    }

}