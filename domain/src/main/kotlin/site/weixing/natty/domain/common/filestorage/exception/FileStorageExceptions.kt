package site.weixing.natty.domain.common.filestorage.exception

/**
 * 文件存储异常基类
 */
sealed class FileStorageException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String = "UNKNOWN_ERROR"
) : RuntimeException(message, cause)

/**
 * 文件上传异常
 */
class FileUploadException(
    message: String,
    cause: Throwable? = null,
    errorCode: String = "UPLOAD_ERROR"
) : FileStorageException(message, cause, errorCode) {
    
    companion object {
        const val FILE_TOO_LARGE = "FILE_TOO_LARGE"
        const val INVALID_FILE_TYPE = "INVALID_FILE_TYPE"
        const val INVALID_FILE_NAME = "INVALID_FILE_NAME"
        const val STORAGE_QUOTA_EXCEEDED = "STORAGE_QUOTA_EXCEEDED"
        const val UPLOAD_INTERRUPTED = "UPLOAD_INTERRUPTED"
        
        fun fileTooLarge(maxSize: Long, actualSize: Long): FileUploadException {
            return FileUploadException(
                "文件大小超过限制: ${formatSize(actualSize)} > ${formatSize(maxSize)}",
                errorCode = FILE_TOO_LARGE
            )
        }
        
        fun invalidFileType(fileName: String, contentType: String): FileUploadException {
            return FileUploadException(
                "不支持的文件类型: $fileName ($contentType)",
                errorCode = INVALID_FILE_TYPE
            )
        }
        
        fun invalidFileName(fileName: String): FileUploadException {
            return FileUploadException(
                "无效的文件名: $fileName",
                errorCode = INVALID_FILE_NAME
            )
        }
        
        fun storageQuotaExceeded(requiredSize: Long, availableSize: Long): FileUploadException {
            return FileUploadException(
                "存储配额不足: 需要 ${formatSize(requiredSize)}, 可用 ${formatSize(availableSize)}",
                errorCode = STORAGE_QUOTA_EXCEEDED
            )
        }
        
        private fun formatSize(bytes: Long): String {
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
}

/**
 * 文件处理异常
 */
class FileProcessingException(
    message: String,
    cause: Throwable? = null,
    errorCode: String = "PROCESSING_ERROR",
    val processingType: String = "UNKNOWN"
) : FileStorageException(message, cause, errorCode) {
    
    companion object {
        const val PROCESSOR_NOT_FOUND = "PROCESSOR_NOT_FOUND"
        const val PROCESSING_TIMEOUT = "PROCESSING_TIMEOUT"
        const val PROCESSING_FAILED = "PROCESSING_FAILED"
        const val INVALID_PROCESSING_OPTIONS = "INVALID_PROCESSING_OPTIONS"
        const val UNSUPPORTED_OPERATION = "UNSUPPORTED_OPERATION"
        
        fun processorNotFound(processorType: String): FileProcessingException {
            return FileProcessingException(
                "未找到处理器: $processorType",
                errorCode = PROCESSOR_NOT_FOUND,
                processingType = processorType
            )
        }
        
        fun processingTimeout(processingType: String, timeoutMs: Long): FileProcessingException {
            return FileProcessingException(
                "处理超时: $processingType (${timeoutMs}ms)",
                errorCode = PROCESSING_TIMEOUT,
                processingType = processingType
            )
        }
        
        fun processingFailed(processingType: String, reason: String, cause: Throwable? = null): FileProcessingException {
            return FileProcessingException(
                "处理失败: $processingType - $reason",
                cause = cause,
                errorCode = PROCESSING_FAILED,
                processingType = processingType
            )
        }
        
        fun unsupportedOperation(processingType: String, fileName: String): FileProcessingException {
            return FileProcessingException(
                "不支持的操作: $processingType 无法处理文件 $fileName",
                errorCode = UNSUPPORTED_OPERATION,
                processingType = processingType
            )
        }
    }
}

/**
 * 存储异常
 */
class StorageException(
    message: String,
    cause: Throwable? = null,
    errorCode: String = "STORAGE_ERROR",
    val provider: String = "UNKNOWN"
) : FileStorageException(message, cause, errorCode) {
    
    companion object {
        const val STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE"
        const val STORAGE_ACCESS_DENIED = "STORAGE_ACCESS_DENIED"
        const val STORAGE_IO_ERROR = "STORAGE_IO_ERROR"
        const val STORAGE_CONFIGURATION_ERROR = "STORAGE_CONFIGURATION_ERROR"
        const val FILE_NOT_FOUND = "FILE_NOT_FOUND"
        const val FILE_ALREADY_EXISTS = "FILE_ALREADY_EXISTS"
        
        fun storageUnavailable(provider: String, cause: Throwable? = null): StorageException {
            return StorageException(
                "存储服务不可用: $provider",
                cause = cause,
                errorCode = STORAGE_UNAVAILABLE,
                provider = provider
            )
        }
        
        fun fileNotFound(path: String, provider: String): StorageException {
            return StorageException(
                "文件未找到: $path",
                errorCode = FILE_NOT_FOUND,
                provider = provider
            )
        }
        
        fun fileAlreadyExists(path: String, provider: String): StorageException {
            return StorageException(
                "文件已存在: $path",
                errorCode = FILE_ALREADY_EXISTS,
                provider = provider
            )
        }
        
        fun accessDenied(path: String, provider: String): StorageException {
            return StorageException(
                "访问被拒绝: $path",
                errorCode = STORAGE_ACCESS_DENIED,
                provider = provider
            )
        }
    }
}

/**
 * 验证异常
 */
class ValidationException(
    message: String,
    errorCode: String = "VALIDATION_ERROR",
    val field: String? = null
) : FileStorageException(message, errorCode = errorCode) {
    
    companion object {
        const val REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING"
        const val INVALID_FORMAT = "INVALID_FORMAT"
        const val VALUE_OUT_OF_RANGE = "VALUE_OUT_OF_RANGE"
        const val INVALID_REFERENCE = "INVALID_REFERENCE"
        
        fun requiredFieldMissing(fieldName: String): ValidationException {
            return ValidationException(
                "必填字段缺失: $fieldName",
                errorCode = REQUIRED_FIELD_MISSING,
                field = fieldName
            )
        }
        
        fun invalidFormat(fieldName: String, value: String): ValidationException {
            return ValidationException(
                "字段格式无效: $fieldName = $value",
                errorCode = INVALID_FORMAT,
                field = fieldName
            )
        }
        
        fun valueOutOfRange(fieldName: String, value: Any, min: Any?, max: Any?): ValidationException {
            val rangeText = when {
                min != null && max != null -> "[$min, $max]"
                min != null -> ">= $min"
                max != null -> "<= $max"
                else -> "有效范围内"
            }
            return ValidationException(
                "字段值超出范围: $fieldName = $value, 应在 $rangeText",
                errorCode = VALUE_OUT_OF_RANGE,
                field = fieldName
            )
        }
        
        fun invalidReference(referenceId: String): ValidationException {
            return ValidationException(
                "无效的引用: $referenceId",
                errorCode = INVALID_REFERENCE
            )
        }
    }
}

/**
 * 配置异常
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null,
    errorCode: String = "CONFIGURATION_ERROR",
    val configKey: String? = null
) : FileStorageException(message, cause, errorCode) {
    
    companion object {
        const val MISSING_CONFIGURATION = "MISSING_CONFIGURATION"
        const val INVALID_CONFIGURATION = "INVALID_CONFIGURATION"
        const val CONFIGURATION_CONFLICT = "CONFIGURATION_CONFLICT"
        
        fun missingConfiguration(configKey: String): ConfigurationException {
            return ConfigurationException(
                "缺少配置: $configKey",
                errorCode = MISSING_CONFIGURATION,
                configKey = configKey
            )
        }
        
        fun invalidConfiguration(configKey: String, value: String, reason: String): ConfigurationException {
            return ConfigurationException(
                "无效配置: $configKey = $value ($reason)",
                errorCode = INVALID_CONFIGURATION,
                configKey = configKey
            )
        }
    }
}

/**
 * 权限异常
 */
class PermissionException(
    message: String,
    errorCode: String = "PERMISSION_ERROR",
    val userId: String? = null,
    val resource: String? = null,
    val action: String? = null
) : FileStorageException(message, errorCode = errorCode) {
    
    companion object {
        const val ACCESS_DENIED = "ACCESS_DENIED"
        const val INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS"
        const val AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED"
        
        fun accessDenied(userId: String?, resource: String, action: String): PermissionException {
            return PermissionException(
                "访问被拒绝: 用户 $userId 无权对资源 $resource 执行 $action 操作",
                errorCode = ACCESS_DENIED,
                userId = userId,
                resource = resource,
                action = action
            )
        }
        
        fun authenticationRequired(resource: String): PermissionException {
            return PermissionException(
                "需要认证才能访问资源: $resource",
                errorCode = AUTHENTICATION_REQUIRED,
                resource = resource
            )
        }
    }
}

/**
 * 存储配置异常类别名
 */
typealias StorageConfigurationException = ConfigurationException

/**
 * 存储提供者不可用异常类别名  
 */
typealias StorageProviderUnavailableException = StorageException