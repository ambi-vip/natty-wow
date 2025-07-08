package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.service.TempFileStorageService
import java.util.UUID

/**
 * 文件上传应用服务
 * 协调文件上传的完整流程：临时存储 -> 领域处理 -> 物理存储
 */
@Service
class FileUploadApplicationService(
    private val tempFileStorageService: TempFileStorageService,
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
        logger.info { "开始处理文件上传: ${request.fileName}" }
        
        return Mono.fromCallable {
            // 1. 将文件内容存储到临时存储
            val tempFileId = tempFileStorageService.storeTempFile(
                fileContent = request.fileContent,
                fileName = request.fileName,
                contentType = request.contentType
            )
            
            logger.debug { "文件内容已存储到临时存储: $tempFileId for ${request.fileName}" }
            
            // 2. 将临时文件ID存储到全局映射中，供事件处理器使用
            FileContentRegistry.storeTempFileMapping(tempFileId, request.fileContent)
            
            tempFileId
        }
        .flatMap { tempFileId ->
            // 3. 生成文件ID并发送命令到聚合根
            val fileId = UUID.randomUUID().toString()
            
            val uploadCommand = UploadFile(
                fileName = request.fileName,
                folderId = request.folderId,
                uploaderId = request.uploaderId,
                fileSize = request.fileSize,
                contentType = request.contentType,
                fileContent = request.fileContent, // 仍然需要在命令中包含，用于校验
                checksum = request.checksum,
                isPublic = request.isPublic,
                tags = request.tags,
                customMetadata = request.customMetadata,
                replaceIfExists = request.replaceIfExists
            )
            
            commandGateway.send(uploadCommand.toCommandMessage(aggregateId = fileId))
                .map { fileId }
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

/**
 * 文件内容注册表
 * 临时解决方案：用于在命令处理和事件处理之间传递文件内容
 * 生产环境应该使用 Redis 或其他分布式缓存
 */
object FileContentRegistry {
    private val tempFileContentMap = mutableMapOf<String, ByteArray>()
    
    fun storeTempFileMapping(tempFileId: String, content: ByteArray) {
        tempFileContentMap[tempFileId] = content
        logger.debug { "存储临时文件内容映射: $tempFileId" }
    }
    
    fun getTempFileContent(tempFileId: String): ByteArray? {
        return tempFileContentMap[tempFileId]
    }
    
    fun removeTempFileContent(tempFileId: String) {
        tempFileContentMap.remove(tempFileId)
        logger.debug { "移除临时文件内容映射: $tempFileId" }
    }
    
    private val logger = KotlinLogging.logger {}
} 