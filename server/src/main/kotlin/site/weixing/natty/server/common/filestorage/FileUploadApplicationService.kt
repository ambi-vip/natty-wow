package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.pipeline.FileUploadPipeline
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.router.IntelligentStorageRouter
import site.weixing.natty.domain.common.filestorage.service.LocalFileStorageService
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * 文件上传应用服务
 * 协调文件上传的完整流程：权限验证 -> 领域处理（直接存储）
 */
@Service
class FileUploadApplicationService(
    private val commandGateway: CommandGateway,
    private val storageRouter: IntelligentStorageRouter,
    private val localFileStorageService: LocalFileStorageService
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 处理文件上传请求
     * @param request 文件上传请求
     * @return 文件ID
     */
    fun uploadFile(request: FileUploadRequest): Mono<String> {
        logger.info { "开始处理文件上传: ${request.fileName} (大小: ${request.fileSize} bytes)" }
        
        return Mono.fromCallable {
            // 基本验证
            require(request.fileName.isNotBlank()) { "文件名不能为空" }
            require(request.fileSize > 0) { "文件大小必须大于0" }
            require(request.fileContent.size.toLong() == request.fileSize) { "文件内容大小与声明不匹配" }
            
            // 生成文件ID
            UUID.randomUUID().toString()
        }
        .flatMap { fileId ->
            // 创建上传命令并发送到聚合根
            val uploadCommand = UploadFile(
                fileName = request.fileName,
                folderId = request.folderId,
                uploaderId = request.uploaderId,
                fileSize = request.fileSize,
                contentType = request.contentType,
                fileContent = request.fileContent,
                checksum = request.checksum,
                isPublic = request.isPublic,
                tags = request.tags,
                customMetadata = request.customMetadata,
                replaceIfExists = request.replaceIfExists
            )

            // 发送命令到File聚合根，聚合根将直接处理存储
            commandGateway.sendAndWaitForSnapshot(uploadCommand.toCommandMessage(aggregateId = fileId))
                .then(Mono.just(fileId))
        }
        .doOnSuccess { fileId ->
            logger.info { "文件上传命令发送成功: ${request.fileName} -> $fileId" }
        }
        .doOnError { error ->
            logger.error(error) { "文件上传处理失败: ${request.fileName}" }
        }
    }
    
    /**
     * 处理流式文件上传请求
     * @param request 文件上传请求（包含inputStream）
     * @return 文件ID
     */
    fun uploadFileStream(request: FileUploadRequest): Mono<String> {
        logger.info { "开始处理流式文件上传: ${request.fileName} (大小: ${request.fileSize} bytes)" }
        
        return Mono.fromCallable {
            // 基本验证
            require(request.fileName.isNotBlank()) { "文件名不能为空" }
            require(request.fileSize > 0) { "文件大小必须大于0" }
            require(request.inputStream != null) { "流式上传必须提供inputStream" }
            
            // 生成文件ID
            UUID.randomUUID().toString()
        }
        .flatMap { fileId ->
            // 创建文件上传上下文
            val uploadContext = FileUploadContext(
                fileName = request.fileName,
                fileSize = request.fileSize,
                contentType = request.contentType,
                uploaderId = request.uploaderId,
                folderId = request.folderId,
                isPublic = request.isPublic,
                tags = request.tags,
                customMetadata = request.customMetadata,
                replaceIfExists = request.replaceIfExists
            )
            
            // 选择存储策略
            storageRouter.selectOptimalStrategy(uploadContext)
                .flatMap { strategy ->
                    // 创建处理上下文
                    val context = ProcessingContext(
                        fileName = request.fileName,
                        fileSize = request.fileSize,
                        contentType = request.contentType,
                        uploaderId = request.uploaderId,
                        metadata = request.customMetadata.toMutableMap()
                    )
                    
                    // 使用文件上传管道处理流式上传
                    val uploadPipeline = site.weixing.natty.domain.common.filestorage.file.File.createDefaultPipeline()
                    
                    uploadPipeline.processUpload(request.inputStream!!, strategy, context)
                        .flatMap { pipelineResult ->
                            // 计算校验和（如果管道没有提供）
                            val finalChecksum = request.checksum ?: calculateSHA256(pipelineResult.toByteArray())
                            
                            // 创建上传命令
                            val uploadCommand = UploadFile(
                                fileName = request.fileName,
                                folderId = request.folderId,
                                uploaderId = request.uploaderId,
                                fileSize = pipelineResult.getTotalBytes(),
                                contentType = request.contentType,
                                fileContent = pipelineResult.toByteArray(),
                                checksum = finalChecksum,
                                isPublic = request.isPublic,
                                tags = request.tags,
                                customMetadata = request.customMetadata + mapOf(
                                    "streamProcessed" to "true",
                                    "pipelineSuccess" to pipelineResult.success.toString()
                                ),
                                replaceIfExists = request.replaceIfExists
                            )
                            
                            // 发送命令到File聚合根
                            commandGateway.sendAndWaitForSnapshot(uploadCommand.toCommandMessage(aggregateId = fileId))
                                .then(Mono.just(fileId))
                        }
                }
                .onErrorResume { error ->
                    logger.warn("流式上传失败，回退到默认策略: ${error.message}")
                    // 回退到默认策略
                    val defaultStrategy = localFileStorageService.defaultStrategy()
                    
                    val simpleUploadCommand = UploadFile(
                        fileName = request.fileName,
                        folderId = request.folderId,
                        uploaderId = request.uploaderId,
                        fileSize = request.fileSize,
                        contentType = request.contentType,
                        fileContent = request.inputStream?.readBytes() ?: ByteArray(0),
                        checksum = request.checksum,
                        isPublic = request.isPublic,
                        tags = request.tags,
                        customMetadata = request.customMetadata + mapOf("fallbackUpload" to "true"),
                        replaceIfExists = request.replaceIfExists
                    )
                    
                    commandGateway.sendAndWaitForSnapshot(simpleUploadCommand.toCommandMessage(aggregateId = fileId))
                        .then(Mono.just(fileId))
                }
        }
        .doOnSuccess { fileId ->
            logger.info { "流式文件上传命令发送成功: ${request.fileName} -> $fileId" }
        }
        .doOnError { error ->
            logger.error(error) { "流式文件上传处理失败: ${request.fileName}" }
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
    val fileContent: ByteArray,
    val checksum: String? = null,
    val isPublic: Boolean = false,
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap(),
    val replaceIfExists: Boolean = false,
    val inputStream: InputStream? = null // 可选的流式输入
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileUploadRequest

        if (fileName != other.fileName) return false
        if (folderId != other.folderId) return false
        if (uploaderId != other.uploaderId) return false
        if (fileSize != other.fileSize) return false
        if (contentType != other.contentType) return false
        if (!fileContent.contentEquals(other.fileContent)) return false
        if (checksum != other.checksum) return false
        if (isPublic != other.isPublic) return false
        if (tags != other.tags) return false
        if (customMetadata != other.customMetadata) return false
        if (replaceIfExists != other.replaceIfExists) return false
        if (inputStream != other.inputStream) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + uploaderId.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + isPublic.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + customMetadata.hashCode()
        result = 31 * result + replaceIfExists.hashCode()
        result = 31 * result + (inputStream?.hashCode() ?: 0)
        return result
    }
}

 