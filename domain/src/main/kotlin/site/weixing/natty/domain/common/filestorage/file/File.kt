package site.weixing.natty.domain.common.filestorage.file

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.api.command.CommandResultAccessor
import me.ahoo.wow.id.GlobalIdGenerator
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.pipeline.FileUploadPipeline
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingOptions
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.ChecksumProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.CompressionProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.EncryptionProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.ThumbnailProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.VirusScanProcessor
import site.weixing.natty.domain.common.filestorage.router.AccessPattern
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.router.IntelligentStorageRouter
import site.weixing.natty.domain.common.filestorage.router.Priority
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileTransaction
import java.io.InputStream
import java.security.MessageDigest

/**
 * 文件聚合根
 * 管理文件的完整生命周期
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class File(
    private val state: FileState
) {

    companion object {
        private val logger = logger {}
        /**
         * 创建默认的流式处理管道
         */
        fun createDefaultPipeline(): FileUploadPipeline {
            val processors: List<StreamProcessor> = listOf(
                VirusScanProcessor(),
                ChecksumProcessor(),
                CompressionProcessor(),
                EncryptionProcessor(),
                ThumbnailProcessor()
            )

            return FileUploadPipeline(processors)
        }
    }

    // 使用默认实现，后续可通过配置注入
    private val uploadPipeline: FileUploadPipeline = createDefaultPipeline()

    @OnCommand
    fun onUpload(
        command: UploadFile,
        fileStorageService: FileStorageService,
        temporaryFileManager: TemporaryFileManager,
        temporaryFileTransaction: TemporaryFileTransaction,
        commandResultAccessor: CommandResultAccessor
    ): Mono<FileUploaded> {
        val start = System.currentTimeMillis()
        return temporaryFileTransaction.executeWithCleanup(command.temporaryFileReference) {
            buildContext(command)
                .publishOn(Schedulers.boundedElastic())
                .flatMap { uploadContext ->
                    fetchAndValidateTempFile(command, temporaryFileManager)
                        .publishOn(Schedulers.boundedElastic())
                        .map { tempFileRef -> uploadContext to tempFileRef }
                }
                .flatMap { (uploadContext, tempFileRef) ->
                    val beforePipeline = System.currentTimeMillis()
                    val dataBufferFlux = temporaryFileManager.getFileStreamAsFlux(command.temporaryFileReference)

                    val processingContext = createProcessingContext(command)
                    val processedFlux = uploadPipeline.processUpload(dataBufferFlux, processingContext)
                    val afterPipeline = System.currentTimeMillis()
                    logger.debug { "[File.onUpload] Pipeline处理耗时: ${afterPipeline - beforePipeline} ms" }

                    fileStorageService.uploadFile(
                        filePath = generateStoragePath(command.folderId, command.fileName),
                        dataBufferFlux = processedFlux,
                        contentType = command.contentType,
                        metadata = buildStorageMetadata(command, "", tempFileRef)
                    ).doOnSuccess { logger.debug { "[File.onUpload] 存储策略写入耗时: ${System.currentTimeMillis() - afterPipeline} ms" } }

                }
                .map { storageInfo: StorageInfo ->
                    buildFileUploadedEvent(command, storageInfo, "")
                }
                .doOnNext { event ->
                    commandResultAccessor.setCommandResult("actualStoragePath", event.actualStoragePath)
                    commandResultAccessor.setCommandResult("folderId", event.folderId)
                    commandResultAccessor.setCommandResult("checksum", event.checksum)
                }
                .doOnSuccess {
                    val end = System.currentTimeMillis()
                    logger.debug { "[File.onUpload] 总耗时: ${end - start} ms" }
                }
        }
    }


    // 拆分的私有方法实现
    private fun buildContext(command: UploadFile): Mono<FileUploadContext> {
        return Mono.fromCallable {
            createUploadContext(command)
        }
    }

    private fun fetchAndValidateTempFile(
        command: UploadFile,
        temporaryFileManager: TemporaryFileManager
    ): Mono<TemporaryFileReference> {
        return temporaryFileManager.getTemporaryFileReference(command.temporaryFileReference)
            .flatMap { tempFileRef ->
                require(tempFileRef.fileSize == command.fileSize) {
                    "临时文件大小与命令声明不匹配: ${tempFileRef.fileSize} != ${command.fileSize}"
                }
                Mono.just(tempFileRef)
            }
    }

    // 新版：全链路流式处理，无同步IO
    private fun processPipelineAndStore(
        uploadContext: FileUploadContext,
        tempFileRef: TemporaryFileReference,
        command: UploadFile,
        intelligentStorageRouter: IntelligentStorageRouter,
        temporaryFileManager: TemporaryFileManager
    ): Mono<StorageInfo> {
        val dataBufferFlux = temporaryFileManager.getFileStreamAsFlux(command.temporaryFileReference)
        return intelligentStorageRouter.selectOptimalStrategy(uploadContext)
            .flatMap { strategy ->
                val processingContext = createProcessingContext(command)
                val processedFlux = uploadPipeline.processUpload(dataBufferFlux, processingContext)
                strategy
                    .uploadFile(
                        filePath = generateStoragePath(command.folderId, command.fileName),
                        dataBufferFlux = processedFlux,
                        contentType = command.contentType,
                        metadata = buildStorageMetadata(command, "", tempFileRef)
                    )
            }
    }

    private fun buildFileUploadedEvent(
        command: UploadFile,
        storageInfo: StorageInfo,
        checksum: String
    ): FileUploaded {
        return FileUploaded(
            fileName = command.fileName,
            folderId = command.folderId,
            uploaderId = command.uploaderId,
            fileSize = command.fileSize,
            contentType = command.contentType,
            storagePath = storageInfo.storagePath,
            actualStoragePath = storageInfo.storagePath,
            checksum = checksum,
            isPublic = command.isPublic,
            tags = command.tags,
            customMetadata = command.customMetadata,
            storageProvider = storageInfo.provider.name,
            uploadTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 创建文件上传上下文
     */
    private fun createUploadContext(command: UploadFile): FileUploadContext {
        return FileUploadContext(
            fileName = command.fileName,
            fileSize = command.fileSize,
            contentType = command.contentType,
            uploaderId = command.uploaderId,
            folderId = command.folderId,
            isPublic = command.isPublic,
            expectedAccessPattern = determineAccessPattern(command),
            priorityLevel = determinePriority(command),
            tags = command.tags,
            customMetadata = command.customMetadata,
            replaceIfExists = command.replaceIfExists
        )
    }

    /**
     * 确定访问模式
     */
    private fun determineAccessPattern(command: UploadFile): AccessPattern {
        return when {
            // 根据标签判断
            command.tags.contains("hot") || command.tags.contains("frequent") -> AccessPattern.HOT
            command.tags.contains("cold") || command.tags.contains("archive") -> AccessPattern.COLD

            // 根据文件类型判断
            command.contentType.startsWith("image/") && command.isPublic -> AccessPattern.HOT
            command.contentType.startsWith("video/") -> AccessPattern.WARM
            command.contentType.contains("archive") || command.contentType.contains("zip") -> AccessPattern.COLD

            // 默认温数据
            else -> AccessPattern.WARM
        }
    }

    /**
     * 确定优先级
     */
    private fun determinePriority(command: UploadFile): Priority {
        return when {
            command.tags.contains("urgent") || command.tags.contains("critical") -> Priority.CRITICAL
            command.tags.contains("high") -> Priority.HIGH
            command.tags.contains("low") -> Priority.LOW
            else -> Priority.NORMAL
        }
    }

    /**
     * 构建存储元数据
     */
    private fun buildStorageMetadata(
        command: UploadFile, 
        checksum: String, 
        tempFileRef: site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference? = null
    ): Map<String, String> {
        return buildMap {
            put("originalFileName", command.fileName)
            put("uploaderId", command.uploaderId)
            put("folderId", command.folderId)
            put("contentType", command.contentType)
            put("fileSize", command.fileSize.toString())
            put("checksum", checksum)
            put("isPublic", command.isPublic.toString())
            put("uploadTimestamp", System.currentTimeMillis().toString())

            // 添加临时文件相关信息
            put("temporaryFileReference", command.temporaryFileReference)
            put("streamProcessed", "true")
            
            // 添加临时文件的元数据（如果有）
            tempFileRef?.let { ref ->
                put("tempFileCreatedAt", ref.createdAt.toString())
                put("tempFileExpiresAt", ref.expiresAt.toString())
                put("tempFileOriginalChecksum", ref.checksum ?: "unknown")
            }

            // 添加自定义元数据
            putAll(command.customMetadata)

            // 添加标签
            if (command.tags.isNotEmpty()) {
                put("tags", command.tags.joinToString(","))
            }
        }
    }



    /**
     * 计算文件SHA-256校验和（从字节数组）
     */
    private fun calculateChecksum(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 计算文件SHA-256校验和（从输入流）
     */
    private fun calculateChecksum(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        
        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 生成存储路径
     */
    private fun generateStoragePath(folderId: String, fileName: String): String {
        val timestamp = GlobalIdGenerator.generateAsString()
        val fileExtension = fileName.substringAfterLast('.', "")
        val baseFileName = fileName.substringBeforeLast('.')

        return "folders/$folderId/${timestamp}_${baseFileName}.${fileExtension}"
    }

    /**
     * 验证文件类型是否被允许
     */
    private fun validateFileType(contentType: String, allowedTypes: Set<String>): Boolean {
        if (allowedTypes.isEmpty()) return true // 空集合表示允许所有类型

        return allowedTypes.any { allowedType ->
            when {
                allowedType.endsWith("/*") -> contentType.startsWith(allowedType.removeSuffix("/*"))
                else -> contentType == allowedType
            }
        }
    }

    /**
     * 获取文件大小的人类可读格式
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
     * 创建流式处理上下文
     */
    private fun createProcessingContext(command: UploadFile): ProcessingContext {
        val context = ProcessingContext(
            fileName = command.fileName,
            contentType = command.contentType,
            fileSize = command.fileSize,
            uploaderId = command.uploaderId,
            processingOptions = createProcessingOptions(command)
        )

        // 添加额外的元数据供处理器使用
        context.addMetadata("isPublic", command.isPublic)
        context.addMetadata("tags", command.tags)
        context.addMetadata("folderId", command.folderId)

        return context
    }

    /**
     * 创建处理选项
     */
    private fun createProcessingOptions(command: UploadFile): ProcessingOptions {
        return ProcessingOptions(
            enableVirusScan = !command.tags.contains("skip-scan"),
            enableCompression = command.tags.contains("compress") || shouldCompress(command),
            enableEncryption = !command.isPublic || command.tags.contains("encrypt"),
            enableThumbnail = command.contentType.startsWith("image/"),
            enableChecksumValidation = false
        )
    }

    /**
     * 判断是否应该压缩文件
     */
    private fun shouldCompress(command: UploadFile): Boolean {
        // 大于1MB的文本文件和文档建议压缩
        return command.fileSize > 1024 * 1024L &&
                (command.contentType.startsWith("text/") ||
                        command.contentType.startsWith("application/json") ||
                        command.contentType.startsWith("application/xml"))
    }

    /**
     * 构建增强的存储元数据（包含管道处理信息）
     */
    private fun buildEnhancedStorageMetadata(
        command: UploadFile,
        checksum: String,
        pipelineResult: site.weixing.natty.domain.common.filestorage.pipeline.PipelineResult,
        tempFileRef: TemporaryFileReference
    ): Map<String, String> {
        val baseMetadata = buildStorageMetadata(command, checksum, tempFileRef)
        val pipelineMetadata = extractPipelineMetadata(pipelineResult)

        return baseMetadata + pipelineMetadata + mapOf(
            "pipelineProcessed" to "true",
            "processingTime" to pipelineResult.getProcessingDurationMs().toString(),
            "processedSize" to pipelineResult.getTotalBytes().toString(),
            "memoryOptimized" to "true", // 标记使用了内存优化的临时文件机制
            "streamProcessingEnabled" to "true"
        )
    }

    /**
     * 从管道结果中提取元数据
     */
    private fun extractPipelineMetadata(pipelineResult: site.weixing.natty.domain.common.filestorage.pipeline.PipelineResult): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        // 提取处理器统计信息
        pipelineResult.statistics.forEach { (processorName, stats) ->
            metadata["${processorName}_processed"] = "true"
            metadata["${processorName}_time"] = stats.lastProcessingTime.toString()
        }

        // 为所有已知的处理器添加状态，即使它们没有被执行
        val allProcessorNames = listOf(
            "VirusScanProcessor",
            "ChecksumProcessor",
            "CompressionProcessor",
            "EncryptionProcessor",
            "ThumbnailProcessor"
        )
        allProcessorNames.forEach { processorName ->
            if (!metadata.containsKey("${processorName}_processed")) {
                metadata["${processorName}_processed"] = "false"
            }
        }

        // 提取处理上下文中的元数据
        pipelineResult.context.metadata.forEach { (key, value) ->
            metadata["pipeline_$key"] = value.toString()
        }

        // 添加处理时间和处理大小
        metadata["processingTime"] = pipelineResult.getProcessingDurationMs().toString()
        metadata["processedSize"] = pipelineResult.getTotalBytes().toString()

        return metadata
    }
} 