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
class FileReferenceException(
    message: String,
    val referenceId: String? = null,
    cause: Throwable? = null
) : FileProcessingException(message, cause)

/**
 * 文件引用不存在异常
 */
class FileReferenceNotFoundException(
    val referenceId: String
) : FileReferenceException("文件引用不存在: $referenceId", referenceId)

/**
 * 文件引用已过期异常
 */
class FileReferenceExpiredException(
    val referenceId: String,
    val expirationTime: String
) : FileReferenceException("文件引用已过期: $referenceId, 过期时间: $expirationTime", referenceId)

/**
 * 文件引用访问权限异常
 */
class FileReferenceAccessDeniedException(
    val referenceId: String,
    val userId: String
) : FileReferenceException("用户 $userId 无权访问文件引用: $referenceId", referenceId)

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
    "文件大小超限${fileName?.let { " [$it]" } ?: ""}: ${actualSize}字节 > ${maxAllowedSize}字节"
)

/**
 * 文件类型不支持异常
 */
class UnsupportedFileTypeException(
    val contentType: String,
    val fileName: String? = null,
    val allowedTypes: Set<String> = emptySet()
) : FileProcessingException(
    "文件类型不支持${fileName?.let { " [$it]" } ?: ""}: $contentType" +
    if (allowedTypes.isNotEmpty()) ", 支持的类型: ${allowedTypes.joinToString()}" else ""
)

/**
 * 文件内容损坏异常
 */
class FileContentCorruptedException(
    val fileName: String? = null,
    val checksum: String? = null,
    message: String = "文件内容损坏"
) : FileProcessingException(
    "$message${fileName?.let { " [$it]" } ?: ""}${checksum?.let { ", 校验和: $it" } ?: ""}"
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
    "$message${fileName?.let { " [$it]" } ?: ""}" +
    if (validationErrors.isNotEmpty()) ": ${validationErrors.joinToString("; ")}" else ""
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