package site.weixing.natty.domain.common.filestorage.file

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.domain.common.filestorage.pipeline.FileUploadPipeline
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingOptions
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.processors.*
import site.weixing.natty.domain.common.filestorage.router.AccessPattern
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.router.IntelligentStorageRouter
import site.weixing.natty.domain.common.filestorage.router.Priority
import java.io.ByteArrayInputStream
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

    // 使用默认实现，后续可通过配置注入
    private val uploadPipeline: FileUploadPipeline = createDefaultPipeline()

    @OnCommand
    fun onUpload(
        command: UploadFile,
        intelligentStorageRouter: IntelligentStorageRouter
    ): Mono<FileUploaded> {
        return Mono.fromCallable {
            // 业务规则校验
            validateFileName(command.fileName)

            // 验证文件大小
            require(command.fileContent.size.toLong() == command.fileSize) {
                "文件内容大小与声明大小不匹配"
            }

            // 计算文件校验和（如果未提供）
            val checksum = command.checksum ?: calculateChecksum(command.fileContent)

            // 验证校验和（如果提供了）
            command.checksum?.let { providedChecksum ->
                val calculatedChecksum = calculateChecksum(command.fileContent)
                require(providedChecksum == calculatedChecksum) {
                    "文件校验和不匹配，文件可能已损坏"
                }
            }

            // 创建文件上传上下文
            val uploadContext = createUploadContext(command)

            uploadContext to checksum
        }
            .flatMap { (uploadContext, checksum) ->
                // 使用智能路由器选择最优存储策略
                intelligentStorageRouter.selectOptimalStrategy(uploadContext)
                    .flatMap { strategy ->
                        // 生成存储路径
                        val storagePath = generateStoragePath(command.folderId, command.fileName)

                        // 创建流式处理上下文
                        val processingContext = createProcessingContext(command)

                        // 通过流式处理管道处理文件
                        uploadPipeline.processUpload(
                            inputStream = ByteArrayInputStream(command.fileContent),
                            storageStrategy = strategy,
                            context = processingContext
                        ).flatMap { pipelineResult ->
                            if (pipelineResult.success) {
                                // 使用处理后的数据执行存储
                                strategy.uploadFile(
                                    filePath = storagePath,
                                    inputStream = pipelineResult.toInputStream(),
                                    contentType = command.contentType,
                                    fileSize = pipelineResult.getTotalBytes(),
                                    metadata = buildEnhancedStorageMetadata(command, checksum, pipelineResult)
                                ).map { storageInfo ->
                                    // 返回文件上传完成事件
                                    FileUploaded(
                                        fileName = command.fileName,
                                        folderId = command.folderId,
                                        uploaderId = command.uploaderId,
                                        fileSize = command.fileSize,
                                        contentType = command.contentType,
                                        storagePath = storagePath,
                                        actualStoragePath = storageInfo.storagePath,
                                        checksum = pipelineResult.context.getMetadata<String>("checksum") ?: checksum,
                                        isPublic = command.isPublic,
                                        tags = command.tags,
                                        customMetadata = command.customMetadata + extractPipelineMetadata(pipelineResult) + mapOf(
                                            "pipelineProcessed" to "true"
                                        ),
                                        storageProvider = storageInfo.provider.name,
                                        uploadTimestamp = System.currentTimeMillis()
                                    )
                                }
                            } else {
                                // 处理管道失败，使用原始数据存储
                                strategy.uploadFile(
                                    filePath = storagePath,
                                    inputStream = ByteArrayInputStream(command.fileContent),
                                    contentType = command.contentType,
                                    fileSize = command.fileSize,
                                    metadata = buildStorageMetadata(
                                        command,
                                        checksum
                                    ) + mapOf("pipelineError" to (pipelineResult.error ?: "未知错误"))
                                ).map { storageInfo ->
                                    FileUploaded(
                                        fileName = command.fileName,
                                        folderId = command.folderId,
                                        uploaderId = command.uploaderId,
                                        fileSize = command.fileSize,
                                        contentType = command.contentType,
                                        storagePath = storagePath,
                                        actualStoragePath = storageInfo.storagePath,
                                        checksum = checksum,
                                        isPublic = command.isPublic,
                                        tags = command.tags,
                                        customMetadata = command.customMetadata + mapOf("pipelineProcessed" to "false"),
                                        storageProvider = storageInfo.provider.name,
                                        uploadTimestamp = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                    }
            }
            .onErrorMap { error ->
                // 将技术错误转换为业务异常
                when (error) {
                    is IllegalArgumentException -> error
                    is SecurityException -> IllegalArgumentException("文件安全检查失败: ${error.message}", error)
                    else -> IllegalStateException("文件上传失败: ${error.message}", error)
                }
            }
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
    private fun buildStorageMetadata(command: UploadFile, checksum: String): Map<String, String> {
        return buildMap {
            put("originalFileName", command.fileName)
            put("uploaderId", command.uploaderId)
            put("folderId", command.folderId)
            put("contentType", command.contentType)
            put("fileSize", command.fileSize.toString())
            put("checksum", checksum)
            put("isPublic", command.isPublic.toString())
            put("uploadTimestamp", System.currentTimeMillis().toString())

            // 添加自定义元数据
            putAll(command.customMetadata)

            // 添加标签
            if (command.tags.isNotEmpty()) {
                put("tags", command.tags.joinToString(","))
            }
        }
    }

    /**
     * 验证文件名是否合法
     */
    private fun validateFileName(fileName: String) {
        // 文件名不能包含特殊字符
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        require(fileName.none { it in invalidChars }) {
            "文件名包含非法字符: ${invalidChars.joinToString("")}"
        }

        // 文件名长度限制
        require(fileName.length <= 255) { "文件名长度不能超过255个字符" }

        // 文件名不能以点开头或结尾
        require(!fileName.startsWith(".") && !fileName.endsWith(".")) {
            "文件名不能以点开头或结尾"
        }
    }

    /**
     * 计算文件SHA-256校验和
     */
    private fun calculateChecksum(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 生成存储路径
     */
    private fun generateStoragePath(folderId: String, fileName: String): String {
        val timestamp = System.currentTimeMillis()
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
            enableChecksumValidation = true
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
        pipelineResult: site.weixing.natty.domain.common.filestorage.pipeline.PipelineResult
    ): Map<String, String> {
        val baseMetadata = buildStorageMetadata(command, checksum)
        val pipelineMetadata = extractPipelineMetadata(pipelineResult)

        return baseMetadata + pipelineMetadata + mapOf(
            "pipelineProcessed" to "true",
            "processingTime" to pipelineResult.getProcessingDurationMs().toString(),
            "processedSize" to pipelineResult.getTotalBytes().toString()
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

    companion object {
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
} 