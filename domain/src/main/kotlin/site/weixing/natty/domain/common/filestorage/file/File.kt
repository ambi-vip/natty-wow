package site.weixing.natty.domain.common.filestorage.file

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.api.command.CommandResultAccessor
import me.ahoo.wow.id.GlobalIdGenerator
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import java.time.Duration

/**
 * 文件聚合根
 * 纯业务逻辑聚合根，专注于文件业务决策和核心逻辑
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class File(
    private val state: FileState
) {

    companion object {
        private val logger = logger {}
    }

    /**
     * 文件上传命令处理器
     * 只包含业务逻辑和决策，技术细节委托给领域服务
     */
    @OnCommand
    fun onUpload(
        command: UploadFile,
        fileStorageService: FileStorageService,
        commandResultAccessor: CommandResultAccessor
    ): Mono<FileUploaded> {
        val start = System.currentTimeMillis()
        
        return Mono.fromCallable {
            // 业务验证
            validateUploadCommand(command)
            
            // 决定处理需求（合并命令中的选项和业务规则）
            val processingOptions = mergeProcessingOptions(command)
            
            // 生成存储路径
            val storagePath = generateStoragePath(command.folderId, command.fileName)
            
            // 构建文件元数据
            val metadata = buildFileMetadata(command)
            
            ProcessingDecision(storagePath, metadata, processingOptions)
        }
        .flatMap { decision ->
            // 委托给领域服务执行存储
            fileStorageService.storeFile(
                path = decision.storagePath,
                content = command.content,
                metadata = decision.metadata,
                processingOptions = decision.processingOptions
            )
        }
        .map { storageResult ->
            // 构建业务事件
            buildFileUploadedEvent(command, storageResult)
        }
        .doOnNext { event ->
            // 设置命令结果
            commandResultAccessor.setCommandResult("actualStoragePath", event.actualStoragePath)
            commandResultAccessor.setCommandResult("folderId", event.folderId)
            commandResultAccessor.setCommandResult("checksum", event.checksum)
        }
        .doOnSuccess {
            val end = System.currentTimeMillis()
            logger.debug { "[File.onUpload] 文件上传完成，总耗时: ${end - start} ms" }
        }
        .doOnError { error ->
            logger.error(error) { "[File.onUpload] 文件上传失败: ${command.fileName}" }
        }
    }

    /**
     * 验证上传命令的业务规则
     */
    private fun validateUploadCommand(command: UploadFile) {
        require(command.fileName.isNotBlank()) { "文件名不能为空" }
        require(command.fileSize > 0) { "文件大小必须大于0" }
        require(command.contentType.isNotBlank()) { "文件类型不能为空" }
        require(command.folderId.isNotBlank()) { "文件夹ID不能为空" }
        require(command.uploaderId.isNotBlank()) { "上传者ID不能为空" }
        
        // 文件大小限制（业务规则）
        val maxFileSize = 500 * 1024 * 1024L // 500MB
        require(command.fileSize <= maxFileSize) { 
            "文件大小超过限制: ${formatFileSize(command.fileSize)} > ${formatFileSize(maxFileSize)}" 
        }
        
        // 文件名格式验证
        val forbiddenChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        require(!command.fileName.any { it in forbiddenChars }) {
            "文件名包含非法字符: ${forbiddenChars.joinToString("")}"
        }
    }

    /**
     * 合并处理选项（命令参数 + 业务规则）
     */
    private fun mergeProcessingOptions(command: UploadFile): ProcessingOptions {
        // 从命令中获取用户指定的选项
        val userOptions = command.processingOptions
        
        // 应用业务规则
        return ProcessingOptions(
            requireEncryption = userOptions.requireEncryption || shouldEncrypt(command),
            enableCompression = userOptions.enableCompression || shouldCompress(command),
            generateThumbnail = userOptions.generateThumbnail || shouldGenerateThumbnail(command),
            customProcessors = (userOptions.customProcessors + determineCustomProcessors(command)).distinct(),
            maxProcessingTimeMinutes = userOptions.maxProcessingTimeMinutes
        )
    }

    /**
     * 根据业务规则决定处理需求
     */
    fun determineProcessingNeeds(command: UploadFile): ProcessingOptions {
        return ProcessingOptions(
            requireEncryption = shouldEncrypt(command),
            enableCompression = shouldCompress(command),
            generateThumbnail = shouldGenerateThumbnail(command),
            customProcessors = determineCustomProcessors(command),
            maxProcessingTimeMinutes = 5
        )
    }

    /**
     * 判断是否需要加密
     */
    private fun shouldEncrypt(command: UploadFile): Boolean {
        // 业务规则：私有文件或明确要求加密的文件进行加密
        return !command.isPublic || 
               command.tags.contains("encrypt") || 
               command.tags.contains("secure")
    }

    /**
     * 判断是否需要压缩
     */
    private fun shouldCompress(command: UploadFile): Boolean {
        // 用户明确要求压缩
        if (command.tags.contains("compress")) return true
        
        // 大文件的可压缩类型自动压缩
        val compressibleTypes = setOf(
            "text/", "application/json", "application/xml", 
            "application/javascript", "application/css"
        )
        
        return command.fileSize > 1024 * 1024L && // 大于1MB
               compressibleTypes.any { command.contentType.startsWith(it) }
    }

    /**
     * 判断是否需要生成缩略图
     */
    private fun shouldGenerateThumbnail(command: UploadFile): Boolean {
        // 业务规则：图片文件且为公开文件生成缩略图
        return command.contentType.startsWith("image/") && 
               command.isPublic &&
               !command.tags.contains("no-thumbnail")
    }

    /**
     * 确定自定义处理器
     */
    private fun determineCustomProcessors(command: UploadFile): List<String> {
        val processors = mutableListOf<String>()
        
        // 根据标签决定处理器
        if (command.tags.contains("virus-scan")) {
            processors.add("VirusScanProcessor")
        }
        
        if (command.tags.contains("ocr")) {
            processors.add("OcrProcessor")
        }
        
        if (command.tags.contains("watermark")) {
            processors.add("WatermarkProcessor")
        }
        
        return processors
    }

    /**
     * 生成存储路径（业务规则）
     */
    private fun generateStoragePath(folderId: String, fileName: String): String {
        val timestamp = GlobalIdGenerator.generateAsString()
        val fileExtension = fileName.substringAfterLast('.', "")
        val baseFileName = fileName.substringBeforeLast('.')
        
        // 清理文件名，移除特殊字符
        val cleanFileName = baseFileName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
        
        return "folders/$folderId/${timestamp}_${cleanFileName}.${fileExtension}"
    }

    /**
     * 构建文件元数据
     */
    private fun buildFileMetadata(command: UploadFile): FileMetadata {
        return FileMetadata(
            originalFileName = command.fileName,
            uploaderId = command.uploaderId,
            folderId = command.folderId,
            contentType = command.contentType,
            fileSize = command.fileSize,
            isPublic = command.isPublic,
            tags = command.tags,
            customMetadata = command.customMetadata,
            uploadTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 构建文件上传完成事件
     */
    private fun buildFileUploadedEvent(
        command: UploadFile,
        storageResult: StorageResult
    ): FileUploaded {
        return FileUploaded(
            fileName = command.fileName,
            folderId = command.folderId,
            uploaderId = command.uploaderId,
            fileSize = command.fileSize,
            contentType = command.contentType,
            storagePath = storageResult.storagePath,
            actualStoragePath = storageResult.actualStoragePath,
            checksum = storageResult.checksum,
            isPublic = command.isPublic,
            tags = command.tags,
            customMetadata = command.customMetadata,
            storageProviderId = storageResult.providerId,
            storageProvider = storageResult.providerName,
            uploadTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 格式化文件大小为可读格式
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * 处理决策数据类
     */
    private data class ProcessingDecision(
        val storagePath: String,
        val metadata: FileMetadata,
        val processingOptions: ProcessingOptions
    )
}

/**
 * 文件元数据值对象
 */
data class FileMetadata(
    val originalFileName: String,
    val uploaderId: String,
    val folderId: String,
    val contentType: String,
    val fileSize: Long,
    val isPublic: Boolean,
    val tags: List<String>,
    val customMetadata: Map<String, String>,
    val uploadTimestamp: Long
)

/**
 * 存储结果值对象
 */
data class StorageResult(
    val storagePath: String,
    val actualStoragePath: String,
    val checksum: String,
    val providerId: String,
    val providerName: String
)