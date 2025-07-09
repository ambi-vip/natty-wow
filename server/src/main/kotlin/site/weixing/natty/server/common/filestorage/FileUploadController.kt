package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

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
        private val executorService = Executors.newCachedThreadPool()
    }
    
    /**
     * 上传文件
     * POST /api/files/upload
     */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("file") file: Mono<FilePart>,
        @RequestParam("folderId") folderId: String,
        @RequestParam("uploaderId") uploaderId: String,
        @RequestParam("isPublic", defaultValue = "false") isPublic: Boolean,
        @RequestParam("tags", required = false) tags: List<String>?,
        @RequestParam("replaceIfExists", defaultValue = "false") replaceIfExists: Boolean,
        @RequestParam("customMetadata", required = false) customMetadataJson: String?
    ): Mono<ResponseEntity<FileUploadResponse>> {
        
        return file.flatMap { part ->
            val fileName = part.filename()
            val contentType = part.headers().contentType?.toString() ?: "application/octet-stream"
            
            logger.info { "接收文件上传请求: $fileName (类型: $contentType)" }
            
            // 检查文件大小限制（来自Content-Length header）
            val contentLength = part.headers().contentLength
            if (contentLength > 100 * 1024 * 1024L) { // 100MB 限制
                return@flatMap Mono.error<ResponseEntity<FileUploadResponse>>(
                    IllegalArgumentException("文件大小超过限制（最大100MB）")
                )
            }
            
            // 对于大文件使用流式处理
            if (contentLength > 10 * 1024 * 1024L) { // 10MB以上使用流式处理
                processLargeFileUpload(part, fileName, contentType, folderId, uploaderId, 
                                     isPublic, tags, replaceIfExists, customMetadataJson, contentLength)
            } else {
                // 小文件仍然可以使用内存处理（优化后的方式）
                processSmallFileUpload(part, fileName, contentType, folderId, uploaderId, 
                                     isPublic, tags, replaceIfExists, customMetadataJson)
            }
        }
        .onErrorResume { error ->
            logger.error(error) { "文件上传失败: ${error.message}" }
            val errorResponse = FileUploadResponse(
                fileId = "",
                fileName = "unknown",
                fileSize = 0L,
                contentType = "application/octet-stream",
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
     * 处理大文件上传 - 使用流式处理
     */
    private fun processLargeFileUpload(
        part: FilePart,
        fileName: String,
        contentType: String,
        folderId: String,
        uploaderId: String,
        isPublic: Boolean,
        tags: List<String>?,
        replaceIfExists: Boolean,
        customMetadataJson: String?,
        fileSize: Long
    ): Mono<ResponseEntity<FileUploadResponse>> {
        
        logger.info { "使用流式处理大文件: $fileName (${fileSize / 1024 / 1024}MB)" }
        
        return Mono.fromCallable {
            // 创建管道流进行流式传输
            val pipedOutputStream = PipedOutputStream()
            val pipedInputStream = PipedInputStream(pipedOutputStream, 64 * 1024) // 64KB缓冲区
            
            // 异步写入数据到管道
            executorService.submit {
                try {
                    DataBufferUtils.write(part.content(), pipedOutputStream)
                        .doFinally { pipedOutputStream.close() }
                        .subscribe(
                            { },
                            { error -> 
                                logger.error(error) { "流式写入失败: $fileName" }
                                pipedOutputStream.close()
                            },
                            { pipedOutputStream.close() }
                        )
                } catch (e: Exception) {
                    logger.error(e) { "管道写入异常: $fileName" }
                    try { pipedOutputStream.close() } catch (ignore: Exception) {}
                }
            }
            
            // 对于流式处理，我们使用声明的文件大小，校验和将由管道处理
            val request = FileUploadRequest(
                fileName = fileName,
                folderId = folderId,
                uploaderId = uploaderId,
                fileSize = fileSize,
                contentType = contentType,
                fileContent = ByteArray(0), // 流式处理时不使用byte数组
                checksum = null, // 校验和将由流式处理管道计算
                isPublic = isPublic,
                tags = tags ?: emptyList(),
                customMetadata = parseCustomMetadata(customMetadataJson),
                replaceIfExists = replaceIfExists,
                inputStream = pipedInputStream // 添加输入流
            )
            
            request
        }
        .flatMap { request ->
            fileUploadApplicationService.uploadFileStream(request)
                .map { fileId ->
                    val response = FileUploadResponse(
                        fileId = fileId,
                        fileName = fileName,
                        fileSize = fileSize,
                        contentType = contentType,
                        message = "大文件上传成功"
                    )
                    ResponseEntity.ok(response)
                }
        }
    }
    
    /**
     * 处理小文件上传 - 优化的内存处理
     */
    private fun processSmallFileUpload(
        part: FilePart,
        fileName: String,
        contentType: String,
        folderId: String,
        uploaderId: String,
        isPublic: Boolean,
        tags: List<String>?,
        replaceIfExists: Boolean,
        customMetadataJson: String?
    ): Mono<ResponseEntity<FileUploadResponse>> {
        
        return part.content()
            .collectList() // 收集所有DataBuffer
            .map { dataBuffers ->
                // 计算总大小
                val totalSize = dataBuffers.sumOf { it.readableByteCount() }
                logger.debug { "处理小文件: $fileName (${totalSize}bytes)" }
                
                // 使用ByteArrayOutputStream避免重复拷贝
                val outputStream = ByteArrayOutputStream(totalSize)
                
                try {
                    dataBuffers.forEach { dataBuffer ->
                        val bytes = ByteArray(dataBuffer.readableByteCount())
                        dataBuffer.read(bytes)
                        outputStream.write(bytes)
                        DataBufferUtils.release(dataBuffer) // 释放DataBuffer
                    }
                    
                    outputStream.toByteArray()
                } finally {
                    outputStream.close()
                }
            }
            .flatMap { fileContent ->
                // 验证文件
                require(fileContent.isNotEmpty()) { "文件不能为空" }
                require(fileName.isNotBlank()) { "文件名不能为空" }
                
                // 计算文件校验和
                val checksum = calculateSHA256(fileContent)
                
                val request = FileUploadRequest(
                    fileName = fileName,
                    folderId = folderId,
                    uploaderId = uploaderId,
                    fileSize = fileContent.size.toLong(),
                    contentType = contentType,
                    fileContent = fileContent,
                    checksum = checksum,
                    isPublic = isPublic,
                    tags = tags ?: emptyList(),
                    customMetadata = parseCustomMetadata(customMetadataJson),
                    replaceIfExists = replaceIfExists
                )
                
                fileUploadApplicationService.uploadFile(request)
                    .map { fileId ->
                        val response = FileUploadResponse(
                            fileId = fileId,
                            fileName = fileName,
                            fileSize = fileContent.size.toLong(),
                            contentType = contentType,
                            message = "文件上传成功"
                        )
                        ResponseEntity.ok(response)
                    }
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