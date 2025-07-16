package site.weixing.natty.server.common.filestorage.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.exception.*
import java.time.LocalDateTime

/**
 * 文件存储全局异常处理器
 * 统一处理文件存储相关的异常，提供一致的错误响应格式
 */
@RestControllerAdvice(basePackages = ["site.weixing.natty.server.common.filestorage"])
class FileStorageExceptionHandler {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 处理文件上传异常
     */
    @ExceptionHandler(FileUploadException::class)
    fun handleFileUploadException(ex: FileUploadException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "文件上传异常: ${ex.message}" }
        
        val status = when (ex.errorCode) {
            FileUploadException.FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE
            FileUploadException.INVALID_FILE_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
            FileUploadException.INVALID_FILE_NAME -> HttpStatus.BAD_REQUEST
            FileUploadException.STORAGE_QUOTA_EXCEEDED -> HttpStatus.INSUFFICIENT_STORAGE
            else -> HttpStatus.BAD_REQUEST
        }
        
        val response = ErrorResponse(
            error = "FILE_UPLOAD_ERROR",
            message = ex.message ?: "文件上传失败",
            details = mapOf(
                "errorCode" to ex.errorCode,
                "type" to "FileUploadException"
            )
        )
        
        return Mono.just(ResponseEntity.status(status).body(response))
    }
    
    /**
     * 处理文件处理异常
     */
    @ExceptionHandler(FileProcessingException::class)
    fun handleFileProcessingException(ex: FileProcessingException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "文件处理异常: ${ex.message}" }
        
        val status = when (ex.errorCode) {
            FileProcessingException.PROCESSOR_NOT_FOUND -> HttpStatus.NOT_IMPLEMENTED
            FileProcessingException.PROCESSING_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT
            FileProcessingException.UNSUPPORTED_OPERATION -> HttpStatus.UNPROCESSABLE_ENTITY
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        val response = ErrorResponse(
            error = "FILE_PROCESSING_ERROR",
            message = ex.message ?: "文件处理失败",
            details = mapOf(
                "errorCode" to ex.errorCode,
                "processingType" to ex.processingType,
                "type" to "FileProcessingException"
            )
        )
        
        return Mono.just(ResponseEntity.status(status).body(response))
    }
    
    /**
     * 处理存储异常
     */
    @ExceptionHandler(StorageException::class)
    fun handleStorageException(ex: StorageException): Mono<ResponseEntity<ErrorResponse>> {
        logger.error(ex) { "存储异常: ${ex.message}" }
        
        val status = when (ex.errorCode) {
            StorageException.FILE_NOT_FOUND -> HttpStatus.NOT_FOUND
            StorageException.FILE_ALREADY_EXISTS -> HttpStatus.CONFLICT
            StorageException.STORAGE_ACCESS_DENIED -> HttpStatus.FORBIDDEN
            StorageException.STORAGE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        val response = ErrorResponse(
            error = "STORAGE_ERROR",
            message = ex.message ?: "存储操作失败",
            details = mapOf(
                "errorCode" to ex.errorCode,
                "provider" to ex.provider,
                "type" to "StorageException"
            )
        )
        
        return Mono.just(ResponseEntity.status(status).body(response))
    }
    
    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "验证异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "VALIDATION_ERROR",
            message = ex.message ?: "请求参数验证失败",
            details = buildMap {
                put("errorCode", ex.errorCode)
                put("type", "ValidationException")
                ex.field?.let { put("field", it) }
            }
        )
        
        return Mono.just(ResponseEntity.badRequest().body(response))
    }
    
    /**
     * 处理配置异常
     */
    @ExceptionHandler(ConfigurationException::class)
    fun handleConfigurationException(ex: ConfigurationException): Mono<ResponseEntity<ErrorResponse>> {
        logger.error(ex) { "配置异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "CONFIGURATION_ERROR",
            message = ex.message ?: "配置错误",
            details = buildMap {
                put("errorCode", ex.errorCode)
                put("type", "ConfigurationException")
                ex.configKey?.let { put("configKey", it) }
            }
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response))
    }
    
    /**
     * 处理权限异常
     */
    @ExceptionHandler(PermissionException::class)
    fun handlePermissionException(ex: PermissionException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "权限异常: ${ex.message}" }
        
        val status = when (ex.errorCode) {
            PermissionException.AUTHENTICATION_REQUIRED -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.FORBIDDEN
        }
        
        val response = ErrorResponse(
            error = "PERMISSION_ERROR",
            message = ex.message ?: "权限不足",
            details = buildMap {
                put("errorCode", ex.errorCode)
                put("type", "PermissionException")
                ex.userId?.let { put("userId", it) }
                ex.resource?.let { put("resource", it) }
                ex.action?.let { put("action", it) }
            }
        )
        
        return Mono.just(ResponseEntity.status(status).body(response))
    }
    
    /**
     * 处理通用文件存储异常
     */
    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorageException(ex: FileStorageException): Mono<ResponseEntity<ErrorResponse>> {
        logger.error(ex) { "文件存储异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "FILE_STORAGE_ERROR",
            message = ex.message ?: "文件存储操作失败",
            details = mapOf(
                "errorCode" to ex.errorCode,
                "type" to ex::class.simpleName.orEmpty()
            )
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response))
    }
    
    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "参数异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "INVALID_ARGUMENT",
            message = ex.message ?: "参数无效",
            details = mapOf(
                "type" to "IllegalArgumentException"
            )
        )
        
        return Mono.just(ResponseEntity.badRequest().body(response))
    }
    
    /**
     * 处理状态异常
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn(ex) { "状态异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "INVALID_STATE",
            message = ex.message ?: "操作状态无效",
            details = mapOf(
                "type" to "IllegalStateException"
            )
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response))
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): Mono<ResponseEntity<ErrorResponse>> {
        logger.error(ex) { "未处理的异常: ${ex.message}" }
        
        val response = ErrorResponse(
            error = "INTERNAL_ERROR",
            message = "内部服务器错误",
            details = mapOf(
                "type" to ex::class.simpleName.orEmpty()
            )
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response))
    }
}

/**
 * 统一错误响应格式
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = LocalDateTime.now().toString(),
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun create(
            error: String,
            message: String,
            details: Map<String, Any> = emptyMap()
        ): ErrorResponse {
            return ErrorResponse(
                error = error,
                message = message,
                details = details
            )
        }
    }
}