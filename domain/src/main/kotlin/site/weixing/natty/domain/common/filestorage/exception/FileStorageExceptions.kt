package site.weixing.natty.domain.common.filestorage.exception

/**
 * 文件存储异常基类
 */
abstract class FileStorageException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 文件不存在异常
 */
class FileNotFoundException(
    val filePath: String,
    message: String = "文件不存在: $filePath",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件已存在异常
 */
class FileAlreadyExistsException(
    val filePath: String,
    message: String = "文件已存在: $filePath",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 存储空间不足异常
 */
class StorageSpaceInsufficientException(
    val requiredSpace: Long,
    val availableSpace: Long,
    message: String = "存储空间不足，需要 ${requiredSpace} 字节，可用 ${availableSpace} 字节",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件大小超限异常
 */
class FileSizeExceededException(
    val fileSize: Long,
    val maxSize: Long,
    message: String = "文件大小超限，文件大小 ${fileSize} 字节，最大允许 ${maxSize} 字节",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件类型不支持异常
 */
class UnsupportedFileTypeException(
    val contentType: String,
    val allowedTypes: List<String>,
    message: String = "不支持的文件类型: $contentType，允许的类型: ${allowedTypes.joinToString(", ")}",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 存储配置异常
 */
class StorageConfigurationException(
    val configKey: String,
    message: String = "存储配置错误: $configKey",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 存储连接异常
 */
class StorageConnectionException(
    val provider: String,
    message: String = "无法连接到存储服务: $provider",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件校验异常
 */
class FileValidationException(
    val filePath: String,
    val validationType: String,
    message: String = "文件校验失败: $filePath ($validationType)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件权限异常
 */
class FilePermissionException(
    val filePath: String,
    val operation: String,
    message: String = "文件权限不足: $filePath (操作: $operation)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 存储操作超时异常
 */
class StorageOperationTimeoutException(
    val operation: String,
    val timeoutSeconds: Long,
    message: String = "存储操作超时: $operation (超时时间: ${timeoutSeconds}秒)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件路径无效异常
 */
class InvalidFilePathException(
    val filePath: String,
    val reason: String,
    message: String = "无效的文件路径: $filePath ($reason)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件损坏异常
 */
class FileCorruptedException(
    val filePath: String,
    val expectedChecksum: String,
    val actualChecksum: String,
    message: String = "文件已损坏: $filePath (期望校验和: $expectedChecksum, 实际校验和: $actualChecksum)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 存储配额超限异常
 */
class StorageQuotaExceededException(
    val currentUsage: Long,
    val quota: Long,
    message: String = "存储配额超限，当前使用: ${currentUsage} 字节，配额: ${quota} 字节",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 并发访问冲突异常
 */
class FileConcurrencyException(
    val filePath: String,
    val operation: String,
    message: String = "文件并发访问冲突: $filePath (操作: $operation)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * CDN服务异常
 */
class CdnServiceException(
    val provider: String,
    val operation: String,
    message: String = "CDN服务异常: $provider (操作: $operation)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 文件版本冲突异常
 */
class FileVersionConflictException(
    val filePath: String,
    val currentVersion: Int,
    val expectedVersion: Int,
    message: String = "文件版本冲突: $filePath (当前版本: $currentVersion, 期望版本: $expectedVersion)",
    cause: Throwable? = null
) : FileStorageException(message, cause)

/**
 * 批量操作异常
 */
class BatchOperationException(
    val operation: String,
    val successCount: Int,
    val failureCount: Int,
    val failures: List<String>,
    message: String = "批量操作部分失败: $operation (成功: $successCount, 失败: $failureCount)",
    cause: Throwable? = null
) : FileStorageException(message, cause) {
    
    /**
     * 获取失败详情
     */
    fun getFailureDetails(): String {
        return failures.joinToString("\n") { "- $it" }
    }
    
    /**
     * 是否完全失败
     */
    fun isCompleteFailure(): Boolean {
        return successCount == 0
    }
}

/**
 * 存储提供商不可用异常
 */
class StorageProviderUnavailableException(
    val provider: String,
    val reason: String,
    message: String = "存储提供商不可用: $provider ($reason)",
    cause: Throwable? = null
) : FileStorageException(message, cause) 