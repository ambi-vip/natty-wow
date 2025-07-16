package site.weixing.natty.api.common.filestorage.file

/**
 * 文件上传完成事件（核心业务事件）
 */
data class FileUploaded(
    val fileName: String,
    val folderId: String,
    val uploaderId: String,
    val fileSize: Long,
    val contentType: String,
    val storagePath: String,
    val actualStoragePath: String,
    val checksum: String,
    val isPublic: Boolean,
    val tags: List<String>,
    val customMetadata: Map<String, String>,
    val storageProviderId: String,
    val storageProvider: String,
    val uploadTimestamp: Long,
    val processingRequired: Boolean = false // 是否需要后续处理
)

/**
 * 文件删除事件
 */
data class FileDeleted(
    val fileName: String,
    val storagePath: String,
    val deletedBy: String,
    val deleteTimestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * 文件移动事件
 */
data class FileMoved(
    val fileName: String,
    val oldFolderId: String,
    val newFolderId: String,
    val oldStoragePath: String,
    val newStoragePath: String,
    val movedBy: String,
    val moveTimestamp: Long = System.currentTimeMillis()
)

// ================== 异步处理相关事件 ==================

/**
 * 文件处理开始事件
 */
data class FileProcessingStarted(
    val fileId: String,
    val fileName: String,
    val processingType: ProcessingType,
    val processingOptions: ProcessingOptions,
    val startTimestamp: Long = System.currentTimeMillis()
)

/**
 * 文件处理完成事件
 */
data class FileProcessingCompleted(
    val fileId: String,
    val fileName: String,
    val processingType: ProcessingType,
    val processingResult: ProcessingResult,
    val startTimestamp: Long,
    val endTimestamp: Long = System.currentTimeMillis(),
    val processingDurationMs: Long = endTimestamp - startTimestamp
)

/**
 * 文件处理失败事件
 */
data class FileProcessingFailed(
    val fileId: String,
    val fileName: String,
    val processingType: ProcessingType,
    val errorMessage: String,
    val errorCode: String? = null,
    val startTimestamp: Long,
    val failureTimestamp: Long = System.currentTimeMillis(),
    val retryable: Boolean = true
)

/**
 * 文件状态变更事件
 */
data class FileStatusChanged(
    val fileId: String,
    val fileName: String,
    val oldStatus: FileStatus,
    val newStatus: FileStatus,
    val changedBy: String,
    val changeTimestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * 文件更新事件
 */
data class FileUpdated(
    val fileName: String,
    val folderId: String,
    val updatedBy: String,
    val fileSize: Long,
    val contentType: String,
    val updateTimestamp: Long = System.currentTimeMillis(),
    val changeDescription: String? = null
)

/**
 * 文件复制事件
 */
data class FileCopied(
    val sourceFileName: String,
    val targetFileName: String,
    val sourceFolderId: String,
    val targetFolderId: String,
    val copiedBy: String,
    val copyTimestamp: Long = System.currentTimeMillis()
)

/**
 * 处理类型枚举
 */
enum class ProcessingType {
    ENCRYPTION,      // 加密处理
    COMPRESSION,     // 压缩处理
    THUMBNAIL,       // 缩略图生成
    VIRUS_SCAN,      // 病毒扫描
    OCR,            // 文字识别
    WATERMARK,      // 水印添加
    CUSTOM          // 自定义处理
}

/**
 * 处理结果数据类
 */
data class ProcessingResult(
    val success: Boolean,
    val outputPath: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val artifacts: List<ProcessingArtifact> = emptyList(), // 处理产物（如缩略图）
    val metrics: ProcessingMetrics? = null
)

/**
 * 处理产物
 */
data class ProcessingArtifact(
    val type: String,           // 产物类型：thumbnail, compressed, encrypted等
    val path: String,           // 存储路径
    val size: Long,             // 文件大小
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 处理指标
 */
data class ProcessingMetrics(
    val processingTimeMs: Long,
    val inputSize: Long,
    val outputSize: Long,
    val compressionRatio: Double? = null,
    val qualityScore: Double? = null
)