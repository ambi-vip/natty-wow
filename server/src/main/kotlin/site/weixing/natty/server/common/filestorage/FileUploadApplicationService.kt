package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.UploadFile
import java.util.UUID

/**
 * 文件上传应用服务
 * 协调文件上传的完整流程：权限验证 -> 领域处理（直接存储）
 */
@Service
class FileUploadApplicationService(
    private val commandGateway: CommandGateway
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
    val replaceIfExists: Boolean = false
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
        return result
    }
}

 