package site.weixing.natty.domain.common.filestorage.file

import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference

/**
 * 文件命令工厂
 * 负责构建领域命令，封装业务规则
 */
class FileCommandFactory {

    /**
     * 构建文件上传命令
     * 将应用层请求转换为领域命令，添加业务上下文
     */
    fun buildUploadCommand(
        fileName: String,
        bucketId: String,
        uploaderId: String,
        contentType: String,
        isPublic: Boolean,
        tags: List<String>,
        customMetadata: Map<String, String>,
        processingOptions: ProcessingOptions,
        checksum: String?,
        tempFileRef: TemporaryFileReference
    ): UploadFile {
        return UploadFile(
            fileName = fileName,
            bucketId = bucketId,
            uploaderId = uploaderId,
            fileSize = tempFileRef.fileSize,
            contentType = contentType,
            temporaryFileReference = tempFileRef.referenceId,
            isPublic = isPublic,
            tags = tags,
            customMetadata = enhanceMetadata(customMetadata, contentType),
            processingOptions = processingOptions,
            checksum = checksum
        )
    }

    /**
     * 增强元数据，添加业务上下文信息
     */
    private fun enhanceMetadata(
        originalMetadata: Map<String, String>,
        contentType: String
    ): Map<String, String> {
        return originalMetadata + mapOf(
            "uploadMethod" to "stream",
            "uploadTimestamp" to System.currentTimeMillis().toString(),
            "originalContentType" to contentType,
            "requestSource" to "web-api"
        )
    }
}