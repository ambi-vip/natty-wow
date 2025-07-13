package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.server.common.filestorage.controller.FileUploadResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

/**
 * 文件上传应用服务
 * 协调文件上传的完整流程：权限验证 -> 领域处理（直接存储）
 */
@Service
class FileUploadApplicationService(
    private val commandGateway: CommandGateway,
    private val temporaryFileManager: TemporaryFileManager
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 处理文件上传请求
     * @param request 文件上传请求
     * @return 文件ID
     */
    fun uploadFile(request: FileUploadRequest): Mono<FileUploadResponse> {
        val start = System.currentTimeMillis()
        logger.info { "开始处理文件上传: ${request.fileName} (大小: ${request.fileSize} bytes)" }
        
        return Mono.fromCallable {
            require(request.fileName.isNotBlank()) { "文件名不能为空" }
            UUID.randomUUID().toString()
        }
        .flatMap { fileId ->
            val beforeTemp = System.currentTimeMillis()
            logger.info { "[uploadFile] 生成文件ID耗时: ${beforeTemp - start} ms" }
            temporaryFileManager.createTemporaryFile(
                originalFileName = request.fileName,
                fileSize = request.fileSize,
                contentType = request.contentType,
                dataBufferFlux = request.dataBufferFlux
            ).flatMap { tempFileRef ->
                val afterTemp = System.currentTimeMillis()
                logger.info { "[uploadFile] 创建临时文件耗时: ${afterTemp - beforeTemp} ms" }
                val uploadCommand = UploadFile(
                    fileName = request.fileName,
                    folderId = request.folderId,
                    uploaderId = request.uploaderId,
                    fileSize = tempFileRef.fileSize,
                    contentType = request.contentType,
                    temporaryFileReference = tempFileRef.referenceId,
                    checksum = tempFileRef.checksum,
                    isPublic = request.isPublic,
                    tags = request.tags,
                    customMetadata = request.customMetadata + mapOf(
                        "uploadMethod" to "byteArray",
                        "originalFileSize" to tempFileRef.fileSize.toString()
                    ),
                    replaceIfExists = request.replaceIfExists
                )
                val beforeCmd = System.currentTimeMillis()
                logger.info { "[uploadFile] 构造命令耗时: ${beforeCmd - afterTemp} ms" }
                commandGateway.sendAndWaitForSnapshot(uploadCommand.toCommandMessage(aggregateId = fileId))
                    .doOnSuccess { logger.info { "[uploadFile] 命令处理耗时: ${System.currentTimeMillis() - beforeCmd} ms" } }
                    .flatMap { b -> FileUploadResponse(
                        b.aggregateId,
                        fileName = b.result["actualStoragePath"].toString(),
                        fileSize = 1,
                        uploadMethod = "",
                        message = ""
                    ).toMono() }
                    .onErrorResume { error ->
                        logger.warn(error) { "命令处理失败，清理临时文件: ${tempFileRef.referenceId}" }
                        temporaryFileManager.deleteTemporaryFile(tempFileRef.referenceId)
                            .then(Mono.error(error))
                    }
            }
        }
        .doOnSuccess { data ->
            val end = System.currentTimeMillis()
            logger.info { "[uploadFile] 总耗时: ${end - start} ms, fileId=${data.fileId}" }
        }
        .doOnError { error ->
            logger.error(error) { "文件上传处理失败: ${request.fileName}" }
        }
    }
    
    /**
     * 处理流式文件上传请求（响应式版本）
     * @param request 文件上传请求（必须包含 dataBufferFlux）
     * @return 文件ID
     */
    fun uploadFileOptimized(request: FileUploadRequest): Mono<String> {
        logger.info { "开始处理响应式文件上传: ${request.fileName} (大小: ${request.fileSize} bytes)" }
        // 生成目标路径（示例：本地磁盘）
        val baseDir = System.getProperty("user.dir") + "/storage/files/stream/" + request.folderId
        val targetPath = Paths.get(baseDir, request.fileName)
        return Mono.fromCallable {
            Files.createDirectories(targetPath.parent)
            targetPath
        }.flatMap { path ->
            DataBufferUtils.write(request.dataBufferFlux, path)
                .then(Mono.fromCallable {
                    val fileSize = Files.size(path)
                    // 这里只保存元数据，实际业务可扩展
                    // 生成文件ID
                    val fileId = UUID.randomUUID().toString()
                    fileId
                })
        }
        .doOnSuccess { fileId ->
            logger.info { "响应式文件上传成功: ${request.fileName} -> $fileId" }
        }
        .doOnError { error ->
            logger.error(error) { "响应式文件上传处理失败: ${request.fileName}" }
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

/**
 * 文件上传请求
 */
data class FileUploadRequest(
    val fileName: String,
    val folderId: String,
    val uploaderId: String,
    val fileSize: Long,
    val contentType: String,
    val dataBufferFlux: Flux<DataBuffer>,
    val isPublic: Boolean = false,
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap(),
    val replaceIfExists: Boolean = false
)