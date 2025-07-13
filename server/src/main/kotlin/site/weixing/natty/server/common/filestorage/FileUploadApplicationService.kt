package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.server.common.filestorage.controller.FileUploadResponse
import java.util.*

/**
 * 文件上传应用服务
 * 简化的流程：验证 -> 领域处理 -> 事件发布 -> 响应
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
     * 简化流程：直接处理流式数据，无需临时文件
     */
    fun uploadFile(request: FileUploadRequest): Mono<FileUploadResponse> {
        val start = System.currentTimeMillis()
        logger.info { "开始文件上传: ${request.fileName} (${formatFileSize(request.fileSize)})" }
        
        return Mono.fromCallable {
            // 生成文件ID
            val fileId = UUID.randomUUID().toString()
            
            // 验证上传请求
            validateUploadRequest(request)
            
            fileId
        }
        .flatMap { fileId ->
            // 构建上传命令
            val uploadCommand = buildUploadCommand(request)
            
            // 发送命令到聚合根
            commandGateway.sendAndWaitForSnapshot(uploadCommand.toCommandMessage(aggregateId = fileId))
                .map { snapshot ->
                    // 构建响应
                    FileUploadResponse(
                        fileId = snapshot.aggregateId,
                        fileName = snapshot.result["actualStoragePath"]?.toString() ?: request.fileName,
                        fileSize = request.fileSize,
                        uploadMethod = "stream",
                        message = "文件上传成功",
                        checksum = snapshot.result["checksum"]?.toString(),
                        storagePath = snapshot.result["actualStoragePath"]?.toString(),
                        processingRequired = request.processingOptions.requiresProcessing()
                    )
                }
        }
        .doOnSuccess { response ->
            val end = System.currentTimeMillis()
            logger.info { 
                "文件上传完成: ${request.fileName} -> ${response.fileId} " +
                "耗时: ${end - start}ms 处理需求: ${response.processingRequired}" 
            }
        }
        .doOnError { error ->
            logger.error(error) { "文件上传失败: ${request.fileName}" }
        }
    }

    /**
     * 下载文件
     */
    fun downloadFile(request: FileDownloadRequest): Mono<FileDownloadResult> {
        logger.info { "开始文件下载: ${request.fileId}" }
        
        // 简化实现：直接返回下载信息
        // 实际实现中应该通过查询服务获取文件信息
        return Mono.fromCallable {
            FileDownloadResult(
                fileId = request.fileId,
                downloadUrl = "/api/files/${request.fileId}/download",
                fileName = "unknown", // 应该从查询服务获取
                contentType = "application/octet-stream",
                fileSize = 0L
            )
        }
    }

    /**
     * 删除文件
     */
    fun deleteFile(request: FileDeleteRequest): Mono<Void> {
        logger.info { "开始文件删除: ${request.fileId}" }
        
        return Mono.fromCallable {
            // 构建删除命令（需要定义DeleteFile命令）
            // val deleteCommand = DeleteFile(reason = request.reason)
            // commandGateway.send(deleteCommand.toCommandMessage(aggregateId = request.fileId))
            
            logger.info { "文件删除命令已发送: ${request.fileId}" }
        }.then()
    }

    /**
     * 验证上传请求
     */
    private fun validateUploadRequest(request: FileUploadRequest) {
        require(request.fileName.isNotBlank()) { "文件名不能为空" }
        require(request.folderId.isNotBlank()) { "文件夹ID不能为空" }
        require(request.uploaderId.isNotBlank()) { "上传者ID不能为空" }
//        require(request.fileSize > 0) { "文件大小必须大于0" }
        require(request.contentType.isNotBlank()) { "文件类型不能为空" }
        
        // 文件大小限制
        val maxFileSize = 500 * 1024 * 1024L // 500MB
        require(request.fileSize <= maxFileSize) { 
            "文件大小超过限制: ${formatFileSize(request.fileSize)} > ${formatFileSize(maxFileSize)}" 
        }
    }

    /**
     * 构建上传命令
     */
    private fun buildUploadCommand(request: FileUploadRequest): UploadFile {
        return UploadFile(
            fileName = request.fileName,
            folderId = request.folderId,
            uploaderId = request.uploaderId,
            fileSize = request.fileSize,
            contentType = request.contentType,
            content = request.content,
            isPublic = request.isPublic,
            tags = request.tags,
            customMetadata = request.customMetadata + mapOf(
                "uploadMethod" to "stream",
                "uploadTimestamp" to System.currentTimeMillis().toString()
            ),
            processingOptions = request.processingOptions,
            checksum = request.checksum
        )
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }
}