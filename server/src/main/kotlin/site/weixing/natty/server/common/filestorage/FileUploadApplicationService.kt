package site.weixing.natty.server.common.filestorage

import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference
import site.weixing.natty.api.common.filestorage.file.FileUploadResponse
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.bucket.spec.StorageBucketSpec
import site.weixing.natty.domain.common.filestorage.file.FileCommandFactory
import java.util.*

/**
 * 文件上传应用服务
 * 简化的流程：验证 -> 领域处理 -> 事件发布 -> 响应
 */
@Service
class FileUploadApplicationService(
    private val commandGateway: CommandGateway,
    private val temporaryFileManager: TemporaryFileManager,
    private val fileCommandFactory: FileCommandFactory,
    private val storageBucketSpec: StorageBucketSpec,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(FileUploadApplicationService::class.java)
    }

    /**
     * 处理文件上传请求
     * 协调各个组件完成文件上传流程
     */
    fun uploadFile(request: FileUploadRequest): Mono<FileUploadResponse> {
        val start = System.currentTimeMillis()
        logger.info("开始文件上传: ${request.fileName} (${formatFileSize(request.fileSize)})")

        return prepareUpload(request)
            .flatMap { fileId -> processFileUpload(request, fileId, start) }
            .doOnSuccess { response -> logUploadSuccess(request, response, start) }
            .doOnError { error -> logUploadError(request, error) }
    }

    /**
     * 准备上传：生成文件ID并验证请求
     */
    private fun prepareUpload(request: FileUploadRequest): Mono<String> {
        return Mono.fromCallable {
            val fileId = UUID.randomUUID().toString()
            validateUploadRequest(request)
            storageBucketSpec.validateBucket(request.bucketId, request.fileSize, request.contentType,request.uploaderId)
            fileId
        }
    }

    /**
     * 处理文件上传核心流程
     */
    private fun processFileUpload(
        request: FileUploadRequest,
        fileId: String,
        startTime: Long
    ): Mono<FileUploadResponse> {
        val beforeTemp = System.currentTimeMillis()
        logger.info("[uploadFile] 生成文件ID耗时: ${beforeTemp - startTime} ms")

        return createTemporaryFileAndCommand(request)
            .flatMap { (tempFileRef, uploadCommand) ->
                executeUploadCommand(fileId, uploadCommand, tempFileRef, request)
            }
    }

    /**
     * 创建临时文件并构建上传命令
     */
    private fun createTemporaryFileAndCommand(
        request: FileUploadRequest
    ): Mono<Pair<TemporaryFileReference, UploadFile>> {
        val beforeTemp = System.currentTimeMillis()
        
        return temporaryFileManager.createTemporaryFile(
            originalFileName = request.fileName,
            fileSize = request.fileSize,
            contentType = request.contentType,
            dataBufferFlux = request.dataBufferFlux
        ).map { tempFileRef ->
            val afterTemp = System.currentTimeMillis()
            logger.info("[uploadFile] 创建临时文件耗时: ${afterTemp - beforeTemp} ms")
            
            val uploadCommand = fileCommandFactory.buildUploadCommand(
                request.fileName,
                request.bucketId,
                request.uploaderId,
                request.contentType,
                request.isPublic,
                request.tags,
                request.customMetadata,
                request.processingOptions,
                request.checksum,
                tempFileRef
            )
            
            tempFileRef to uploadCommand
        }
    }

    /**
     * 执行上传命令并处理结果
     */
    private fun executeUploadCommand(
        fileId: String,
        uploadCommand: UploadFile,
        tempFileRef: TemporaryFileReference,
        request: FileUploadRequest
    ): Mono<FileUploadResponse> {
        return commandGateway.sendAndWaitForSnapshot(uploadCommand.toCommandMessage(aggregateId = fileId))
            .map { snapshot -> buildUploadResponse(snapshot, tempFileRef, request) }
            .onErrorResume { error -> handleUploadError(tempFileRef, error) }
    }

    /**
     * 构建上传响应
     */
    private fun buildUploadResponse(
        snapshot: Any,
        tempFileRef: TemporaryFileReference,
        request: FileUploadRequest
    ): FileUploadResponse {
        // 从快照中获取结果，这些结果由聚合根通过CommandResultAccessor设置
        val snapshotClass = snapshot::class.java
        val aggregateIdField = snapshotClass.getDeclaredField("aggregateId").apply { isAccessible = true }
        val resultField = snapshotClass.getDeclaredField("result").apply { isAccessible = true }
        
        val aggregateId = aggregateIdField.get(snapshot).toString()
        @Suppress("UNCHECKED_CAST")
        val result = resultField.get(snapshot) as? Map<String, Any> ?: emptyMap()
        
        return FileUploadResponse(
            fileId = aggregateId,
            fileName = result["actualStoragePath"]?.toString() ?: request.fileName,
            fileSize = tempFileRef.fileSize,
            uploadMethod = "stream",
            message = "文件上传成功",
            checksum = result["checksum"]?.toString(),
            storagePath = result["actualStoragePath"]?.toString(),
            processingRequired = request.processingOptions.requiresProcessing()
        )
    }

    /**
     * 处理上传错误
     */
    private fun handleUploadError(
        tempFileRef: TemporaryFileReference,
        error: Throwable
    ): Mono<FileUploadResponse> {
        logger.warn("命令处理失败，清理临时文件: ${tempFileRef.referenceId}", error)
        return temporaryFileManager.deleteTemporaryFile(tempFileRef.referenceId)
            .then(Mono.error(error))
    }

    /**
     * 记录上传成功日志
     */
    private fun logUploadSuccess(
        request: FileUploadRequest,
        response: FileUploadResponse,
        startTime: Long
    ) {
        val end = System.currentTimeMillis()
        logger.info(
            "文件上传完成: ${request.fileName} -> ${response.fileId} " +
                    "耗时: ${end - startTime}ms 处理需求: ${response.processingRequired}"
        )
    }

    /**
     * 记录上传错误日志
     */
    private fun logUploadError(request: FileUploadRequest, error: Throwable) {
        logger.error("文件上传失败: ${request.fileName}", error)
    }


    /**
     * 验证上传请求（基础验证）
     * 仅验证必要的技术参数，业务规则验证由领域层处理
     */
    private fun validateUploadRequest(request: FileUploadRequest) {
        require(request.fileName.isNotBlank()) { "文件名不能为空" }
        require(request.bucketId.isNotBlank()) { "文件夹ID不能为空" }
        require(request.uploaderId.isNotBlank()) { "上传者ID不能为空" }
        require(request.contentType.isNotBlank()) { "文件类型不能为空" }
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