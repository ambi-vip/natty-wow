package site.weixing.natty.domain.common.filestorage.exception

/**
 * 文件处理异常基类
 */
abstract class FileProcessingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 文件引用相关异常
 */
open class FileReferenceException(
    message: String,
    open val referenceId: String? = null,
    cause: Throwable? = null
) : FileProcessingException(message, cause)

/**
 * 文件处理操作异常
 */
class FileProcessingOperationException(
    val operation: String,
    message: String,
    cause: Throwable? = null
) : FileProcessingException("文件处理操作失败 [$operation]: $message", cause)

/**
 * 文件大小限制异常
 */
class FileSizeLimitExceededException(
    val actualSize: Long,
    val maxAllowedSize: Long,
    val fileName: String? = null
) : FileProcessingException(
    "文件大小超限${fileName?.let { " [$it]" } ?: ""}: ${actualSize}字节 > ${maxAllowedSize}字节",
    null
)

/**
 * 文件类型不支持异常
 */
class UnsupportedFileTypeException(
    val contentType: String,
    val fileName: String? = null,
    val allowedTypes: Set<String> = emptySet()
) : FileProcessingException(
    buildString {
        append("文件类型不支持")
        fileName?.let { append(" [").append(it).append("]") }
        append(": ").append(contentType)
        if (allowedTypes.isNotEmpty()) {
            append(", 支持的类型: ").append(allowedTypes.joinToString())
        }
    },
    null
)

/**
 * 文件内容损坏异常
 */
class FileContentCorruptedException(
    val fileName: String? = null,
    val checksum: String? = null,
    message: String = "文件内容损坏"
) : FileProcessingException(
    buildString {
        append(message)
        fileName?.let { append(" [").append(it).append("]") }
        checksum?.let { append(", 校验和: ").append(it) }
    },
    null
)

/**
 * 存储空间不足异常
 */
class InsufficientStorageSpaceException(
    val requiredSpace: Long,
    val availableSpace: Long
) : FileProcessingException(
    "存储空间不足: 需要 ${requiredSpace}字节, 可用 ${availableSpace}字节"
)

/**
 * 并发处理异常
 */
class ConcurrentFileProcessingException(
    val referenceId: String,
    val currentOperation: String
) : FileProcessingException(
    "文件正在被其他操作处理: $referenceId, 当前操作: $currentOperation"
)

/**
 * 文件处理超时异常
 */
class FileProcessingTimeoutException(
    val timeoutSeconds: Long,
    val operation: String,
    val fileName: String? = null
) : FileProcessingException(
    "文件处理超时${fileName?.let { " [$it]" } ?: ""}: 操作 $operation 超过 ${timeoutSeconds}秒"
)

/**
 * 临时文件清理异常
 */
class TemporaryFileCleanupException(
    val referenceId: String,
    val filePath: String,
    cause: Throwable? = null
) : FileProcessingException(
    "临时文件清理失败: $referenceId -> $filePath", cause
)

/**
 * 文件验证异常
 */
class FileValidationException(
    val fileName: String? = null,
    val validationErrors: List<String> = emptyList(),
    message: String = "文件验证失败"
) : FileProcessingException(
    buildString {
        append(message)
        fileName?.let { append(" [").append(it).append("]") }
        if (validationErrors.isNotEmpty()) {
            append(": ").append(validationErrors.joinToString("; "))
        }
    },
    null
)

/**
 * 批处理异常
 */
class BatchProcessingException(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: Map<String, Exception> = emptyMap()
) : FileProcessingException(
    "批处理完成，成功: $successCount, 失败: $failureCount, 总计: $totalCount"
) {
    
    /**
     * 获取失败的引用ID列表
     */
    fun getFailedReferenceIds(): List<String> = errors.keys.toList()
    
    /**
     * 获取特定引用ID的错误
     */
    fun getError(referenceId: String): Exception? = errors[referenceId]
    
    /**
     * 是否有失败项
     */
    fun hasFailures(): Boolean = failureCount > 0
    
    /**
     * 是否全部成功
     */
    fun isFullSuccess(): Boolean = failureCount == 0
}

/**
 * 存储配置异常
 */
class StorageConfigurationException(
    val configKey: String? = null,
    message: String,
    cause: Throwable? = null
) : FileProcessingException(
    "存储配置错误${configKey?.let { " [$it]" } ?: ""}: $message", cause
)

/**
 * 存储提供商不可用异常
 */
class StorageProviderUnavailableException(
    val provider: String? = null,
    val reason: String? = null,
    providerName: String = provider ?: "Unknown",
    message: String = reason ?: "存储提供商不可用",
    cause: Throwable? = null
) : FileProcessingException("$message: $providerName", cause) {
    
    // 兼容旧的构造函数
    constructor(
        providerName: String,
        message: String = "存储提供商不可用",
        cause: Throwable? = null
    ) : this(
        provider = providerName,
        reason = message,
        providerName = providerName,
        message = message,
        cause = cause
    )
}

/**
 * 文件未找到异常(自定义)
 */
class FileNotFoundException(
    val filePath: String,
    message: String = "文件未找到",
    cause: Throwable? = null
) : FileProcessingException("$message: $filePath", cause)

/**
 * 自定义文件未找到异常(别名)
 */
typealias CustomFileNotFoundException = FileNotFoundException

/**
 * 文件已存在异常
 */
class FileAlreadyExistsException(
    val filePath: String,
    message: String = "文件已存在",
    cause: Throwable? = null
) : FileProcessingException("$message: $filePath", cause)

/**
 * 文件大小超限异常(别名)
 */
typealias FileSizeExceededException = FileSizeLimitExceededException

/**
 * 存储连接异常
 */
class StorageConnectionException(
    val endpoint: String? = null,
    message: String = "存储连接失败",
    cause: Throwable? = null
) : FileProcessingException(
    "$message${endpoint?.let { " [$it]" } ?: ""}", cause
)

/**
 * 无效文件路径异常
 */
class InvalidFilePathException(
    val path: String,
    val reason: String? = null,
    cause: Throwable? = null
) : FileProcessingException(
    "无效文件路径: $path${reason?.let { " ($it)" } ?: ""}", cause
) 