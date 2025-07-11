package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.validation.FileReferenceValidator
import java.io.InputStream
import org.springframework.http.codec.multipart.FilePart
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

/**
 * 文件上传控制器
 * 
 * 提供优化的文件上传接口，支持：
 * 1. 传统的字节数组上传（兼容性）
 * 2. 流式上传（内存优化）
 * 3. MultipartFile上传（Web友好）
 * 4. 大文件分块上传
 * 5. 文件引用验证
 */
@RestController
@RequestMapping("/api/files")
class FileUploadController(
    private val fileUploadApplicationService: FileUploadApplicationService,
    private val temporaryFileManager: TemporaryFileManager,
    private val fileReferenceValidator: FileReferenceValidator
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }


    /**
     * MultipartFile 文件上传接口（WebFlux下应为FilePart）
     * 
     * Web友好的上传方式，自动处理文件流
     */
    @PostMapping("/upload/multipart", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadMultipartFile(
        @RequestPart("file") file: FilePart,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList(),
        @RequestParam(value = "replaceIfExists", required = false) replaceIfExists: Boolean = false
    ): Mono<ResponseEntity<FileUploadResponse>> {
        // 首先尝试从 headers 获取大小
        val sizeFromHeaders = file.headers().contentLength
//
//        val fileSizeMono = if (sizeFromHeaders > 0) {
//            Mono.just(sizeFromHeaders)
//        } else {
//            // 如果 headers 中没有，则计算实际大小
//            file.content()
//                .map { dataBuffer ->
//                    val size = dataBuffer.readableByteCount()
//                    DataBufferUtils.release(dataBuffer)
//                    size
//                }
//                .reduce(0L, Long::plus)
//        }.block()


        logger.info { "收到 FilePart 上传请求: ${file.filename()} " }

        val uploadRequest = FileUploadRequest(
            fileName = file.filename(),
            folderId = folderId,
            uploaderId = uploaderId,
            fileSize = 1L, // 可选：如需准确大小可先统计
            contentType = "application/octet-stream",
            dataBufferFlux = file.content(), // 直接传递 Flux<DataBuffer>
            isPublic = isPublic,
            tags = tags,
            customMetadata = mapOf(
                "originalFilename" to file.filename(),
                "uploadVia" to "filepart"
            ),
            replaceIfExists = replaceIfExists
        )
        return fileUploadApplicationService.uploadFile(uploadRequest)
            .map { fileId ->
                ResponseEntity.ok(
                    FileUploadResponse(
                        fileId = fileId,
                        fileName = file.filename(),
                        fileSize = -1L, // 可选：如需准确大小可后续查询
                        uploadMethod = "filepart",
                        message = "FilePart 上传成功"
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    FileUploadResponse(
                        fileId = null,
                        fileName = file.filename(),
                        fileSize = -1L,
                        uploadMethod = "filepart",
                        message = "FilePart 上传失败"
                    )
                )
            )
    }
    
    /**
     * 流式文件上传接口（端到端非阻塞）// 需要做权限校验
     * 
     * 兼容 Postman、Web 前端的 multipart/form-data 文件上传
     * 文件将直接流式写入本地磁盘，无中间临时文件
     */
    @PostMapping("/upload/stream", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadStream(
        @RequestPart("file") file: FilePart,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam(value = "isPublic", required = false) isPublic: Boolean = false,
        @RequestParam(value = "tags", required = false) tags: List<String> = emptyList(),
        @RequestParam(value = "replaceIfExists", required = false) replaceIfExists: Boolean = false
    ): Mono<ResponseEntity<FileUploadResponse>> {
        logger.info { "收到流式上传请求: ${file.filename()}" }
        val baseDir = System.getProperty("user.dir") + "/storage/files/stream/" + folderId
        val targetPath = java.nio.file.Paths.get(baseDir, file.filename())
        return Mono.fromCallable {
            Files.createDirectories(targetPath.parent)
            targetPath
        }.flatMap { path ->
            DataBufferUtils.write(file.content(), path)
                .then(Mono.fromCallable {
                    val fileSize = Files.size(path)
                    // 这里只保存元数据，实际业务可扩展
                    ResponseEntity.ok(
                        FileUploadResponse(
                            fileId = path.fileName.toString(),
                            fileName = file.filename(),
                            fileSize = fileSize,
                            uploadMethod = "stream",
                            message = "流式上传成功"
                        )
                    )
                })
        }.onErrorReturn(
            ResponseEntity.badRequest().body(
                FileUploadResponse(
                    fileId = null,
                    fileName = file.filename(),
                    fileSize = -1L,
                    uploadMethod = "stream",
                    message = "流式上传失败"
                )
            )
        )
    }
    
    /**
     * 大文件分块上传初始化
     */
    @PostMapping("/upload/chunked/init")
    fun initChunkedUpload(@RequestBody request: ChunkedUploadInitRequest): Mono<ResponseEntity<ChunkedUploadInitResponse>> {
        logger.info { "初始化分块上传: ${request.fileName}, 总大小: ${request.totalSize}" }
        
        return Mono.fromCallable {
            // 生成上传会话ID
            val sessionId = java.util.UUID.randomUUID().toString()
            
            ChunkedUploadInitResponse(
                sessionId = sessionId,
                chunkSize = 1024 * 1024 * 5, // 5MB 分块
                totalChunks = (request.totalSize + 5242879) / 5242880, // 计算总分块数
                message = "分块上传初始化成功"
            )
        }
        .map { response ->
            ResponseEntity.ok(response)
        }
    }
    
    /**
     * 获取临时文件信息
     */
    @GetMapping("/temp/{referenceId}")
    fun getTemporaryFileInfo(@PathVariable referenceId: String): Mono<ResponseEntity<TemporaryFileInfoResponse>> {
        return temporaryFileManager.getTemporaryFileReference(referenceId)
            .map { tempFile ->
                ResponseEntity.ok(
                    TemporaryFileInfoResponse(
                        referenceId = tempFile.referenceId,
                        originalFileName = tempFile.originalFileName,
                        fileSize = tempFile.fileSize,
                        contentType = tempFile.contentType,
                        createdAt = tempFile.createdAt.toString(),
                        expiresAt = tempFile.expiresAt.toString(),
                        isExpired = tempFile.isExpired(),
                        checksum = tempFile.checksum
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.notFound().build()
            )
    }
    
    /**
     * 清理临时文件
     */
    @DeleteMapping("/temp/{referenceId}")
    fun deleteTemporaryFile(@PathVariable referenceId: String): Mono<ResponseEntity<Map<String, Any>>> {
        return temporaryFileManager.deleteTemporaryFile(referenceId)
            .map { deleted ->
                ResponseEntity.ok(
                    mapOf(
                        "referenceId" to referenceId,
                        "deleted" to deleted,
                        "message" to if (deleted) "临时文件删除成功" else "临时文件不存在"
                    )
                )
            }
    }
    
    /**
     * 验证文件引用
     */
    @GetMapping("/validate/{referenceId}")
    fun validateFileReference(
        @PathVariable referenceId: String,
        @RequestParam(value = "userId", required = false) userId: String?
    ): Mono<ResponseEntity<FileReferenceValidationResponse>> {
        logger.debug { "验证文件引用: $referenceId" }
        
        return fileReferenceValidator.validateReference(referenceId, userId)
            .map { result ->
                if (result.isValid) {
                    ResponseEntity.ok(
                        FileReferenceValidationResponse(
                            referenceId = referenceId,
                            isValid = true,
                            message = "文件引用有效",
                            reference = result.reference?.let { ref ->
                                TemporaryFileInfoResponse(
                                    referenceId = ref.referenceId,
                                    originalFileName = ref.originalFileName,
                                    fileSize = ref.fileSize,
                                    contentType = ref.contentType,
                                    createdAt = ref.createdAt.toString(),
                                    expiresAt = ref.expiresAt.toString(),
                                    isExpired = ref.isExpired(),
                                    checksum = ref.checksum
                                )
                            }
                        )
                    )
                } else {
                    ResponseEntity.badRequest().body(
                        FileReferenceValidationResponse(
                            referenceId = referenceId,
                            isValid = false,
                            message = result.errorMessage ?: "文件引用无效",
                            errorCode = result.errorCode?.name
                        )
                    )
                }
            }
    }
    
    /**
     * 批量验证文件引用
     */
    @PostMapping("/validate/batch")
    fun validateMultipleFileReferences(@RequestBody request: BatchValidationRequest): Mono<ResponseEntity<BatchValidationResponse>> {
        logger.debug { "批量验证 ${request.referenceIds.size} 个文件引用" }
        
        return fileReferenceValidator.validateMultipleReferences(request.referenceIds, request.userId)
            .map { results ->
                val validResults = results.mapValues { (it, result) ->
                    FileReferenceValidationResponse(
                        referenceId = it,
                        isValid = result.isValid,
                        message = if (result.isValid) "有效" else (result.errorMessage ?: "无效"),
                        errorCode = result.errorCode?.name,
                        reference = result.reference?.let { ref ->
                            TemporaryFileInfoResponse(
                                referenceId = ref.referenceId,
                                originalFileName = ref.originalFileName,
                                fileSize = ref.fileSize,
                                contentType = ref.contentType,
                                createdAt = ref.createdAt.toString(),
                                expiresAt = ref.expiresAt.toString(),
                                isExpired = ref.isExpired(),
                                checksum = ref.checksum
                            )
                        }
                    )
                }
                
                ResponseEntity.ok(
                    BatchValidationResponse(
                        totalCount = request.referenceIds.size,
                        validCount = results.values.count { it.isValid },
                        invalidCount = results.values.count { !it.isValid },
                        results = validResults
                    )
                )
            }
    }
    
    /**
     * 清理无效的文件引用
     */
    @PostMapping("/cleanup/invalid")
    fun cleanupInvalidReferences(): Mono<ResponseEntity<Map<String, Any>>> {
        logger.info { "手动触发清理无效文件引用" }
        
        return fileReferenceValidator.cleanupInvalidReferences()
            .map { cleanedCount ->
                ResponseEntity.ok(
                    mapOf(
                        "message" to "清理完成",
                        "cleanedCount" to cleanedCount,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
    }
}

/**
 * 流式文件上传请求
 */
data class FileUploadStreamRequest(
    val fileName: String,
    val folderId: String,
    val uploaderId: String,
    val fileSize: Long,
    val contentType: String,
    val inputStream: InputStream,
    val checksum: String? = null,
    val isPublic: Boolean = false,
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap(),
    val replaceIfExists: Boolean = false
)

/**
 * 文件上传响应
 */
data class FileUploadResponse(
    val fileId: String?,
    val fileName: String?,
    val fileSize: Long,
    val uploadMethod: String,
    val message: String
)

/**
 * 分块上传初始化请求
 */
data class ChunkedUploadInitRequest(
    val fileName: String,
    val totalSize: Long,
    val contentType: String,
    val folderId: String,
    val uploaderId: String
)

/**
 * 分块上传初始化响应
 */
data class ChunkedUploadInitResponse(
    val sessionId: String,
    val chunkSize: Long,
    val totalChunks: Long,
    val message: String
)

/**
 * 临时文件信息响应
 */
data class TemporaryFileInfoResponse(
    val referenceId: String,
    val originalFileName: String,
    val fileSize: Long,
    val contentType: String,
    val createdAt: String,
    val expiresAt: String,
    val isExpired: Boolean,
    val checksum: String?
)

/**
 * 文件引用验证响应
 */
data class FileReferenceValidationResponse(
    val referenceId: String,
    val isValid: Boolean,
    val message: String,
    val errorCode: String? = null,
    val reference: TemporaryFileInfoResponse? = null
)

/**
 * 批量验证请求
 */
data class BatchValidationRequest(
    val referenceIds: List<String>,
    val userId: String? = null
)

/**
 * 批量验证响应
 */
data class BatchValidationResponse(
    val totalCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val results: Map<String, FileReferenceValidationResponse>
) 