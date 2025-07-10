package site.weixing.natty.domain.common.filestorage.validation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileReference
import java.time.LocalDateTime

/**
 * 文件引用验证服务
 * 负责验证临时文件引用的有效性、权限和状态
 */
@Service
class FileReferenceValidator(
    private val temporaryFileManager: TemporaryFileManager
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 验证结果
     */
    data class ValidationResult(
        val isValid: Boolean,
        val reference: TemporaryFileReference? = null,
        val errorCode: ValidationErrorCode? = null,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success(reference: TemporaryFileReference) = ValidationResult(
                isValid = true,
                reference = reference
            )
            
            fun failure(errorCode: ValidationErrorCode, message: String) = ValidationResult(
                isValid = false,
                errorCode = errorCode,
                errorMessage = message
            )
        }
    }
    
    /**
     * 验证错误代码
     */
    enum class ValidationErrorCode {
        REFERENCE_NOT_FOUND,
        REFERENCE_EXPIRED,
        FILE_NOT_EXISTS,
        ACCESS_DENIED,
        VALIDATION_ERROR
    }
    
    /**
     * 验证单个文件引用
     */
    fun validateReference(referenceId: String, userId: String? = null): Mono<ValidationResult> {
        logger.debug { "验证文件引用: $referenceId" }
        
        return temporaryFileManager.getTemporaryFileReference(referenceId)
            .map { reference ->
                // 检查文件是否过期
                if (reference.isExpired()) {
                    logger.warn { "文件引用已过期: $referenceId, 过期时间: ${reference.expiresAt}" }
                    return@map ValidationResult.failure(
                        ValidationErrorCode.REFERENCE_EXPIRED,
                        "文件引用已过期: $referenceId"
                    )
                }
                
                // 检查物理文件是否存在
                if (!temporaryFileManager.isTemporaryFileValid(referenceId).block()!!) {
                    logger.warn { "临时文件不存在: ${reference.temporaryPath}" }
                    return@map ValidationResult.failure(
                        ValidationErrorCode.FILE_NOT_EXISTS,
                        "临时文件不存在: $referenceId"
                    )
                }
                
                // 验证用户权限（如果提供了用户ID）
                if (userId != null && !validateUserAccess(reference, userId)) {
                    logger.warn { "用户 $userId 无权访问文件引用: $referenceId" }
                    return@map ValidationResult.failure(
                        ValidationErrorCode.ACCESS_DENIED,
                        "无权访问该文件引用"
                    )
                }
                
                logger.debug { "文件引用验证成功: $referenceId" }
                ValidationResult.success(reference)
            }
            .switchIfEmpty(
                Mono.just(ValidationResult.failure(
                    ValidationErrorCode.REFERENCE_NOT_FOUND,
                    "文件引用不存在: $referenceId"
                ))
            )
            .onErrorReturn(ValidationResult.failure(
                ValidationErrorCode.VALIDATION_ERROR,
                "验证过程中发生错误"
            ))
    }
    
    /**
     * 批量验证文件引用
     */
    fun validateMultipleReferences(
        referenceIds: List<String>,
        userId: String? = null
    ): Mono<Map<String, ValidationResult>> {
        logger.debug { "批量验证 ${referenceIds.size} 个文件引用" }
        
        return Mono.fromCallable {
            referenceIds.associateWith { referenceId: String ->
                validateReference(referenceId, userId).block()!!
            }
        }
    }
    
    /**
     * 验证文件引用是否可以用于特定操作
     */
    fun validateForOperation(
        referenceId: String,
        operation: FileOperation,
        userId: String? = null
    ): Mono<ValidationResult> {
        return validateReference(referenceId, userId)
            .map { result ->
                if (!result.isValid) {
                    return@map result
                }
                
                val reference = result.reference!!
                
                // 根据操作类型进行额外验证
                when (operation) {
                    FileOperation.UPLOAD -> {
                        if (reference.fileSize <= 0) {
                            ValidationResult.failure(
                                ValidationErrorCode.VALIDATION_ERROR,
                                "无效的文件大小"
                            )
                        } else {
                            result
                        }
                    }
                    FileOperation.PROCESS -> {
                        // 处理操作的额外验证
                        result
                    }
                    FileOperation.DELETE -> {
                        // 删除操作的额外验证
                        result
                    }
                }
            }
    }
    
    /**
     * 清理无效的文件引用
     */
    fun cleanupInvalidReferences(): Mono<Int> {
        logger.info { "开始清理无效的文件引用" }
        
        return temporaryFileManager.cleanupExpiredFiles()
            .map { cleanedCount -> cleanedCount.toInt() }
    }
    
    /**
     * 验证用户访问权限
     */
    private fun validateUserAccess(reference: TemporaryFileReference, userId: String): Boolean {
        // 简单的权限检查：目前暂不实现用户权限验证
        // 可以在未来版本中通过添加metadata字段或其他方式实现
        return true
    }
    
    /**
     * 文件操作类型
     */
    enum class FileOperation {
        UPLOAD,
        PROCESS,
        DELETE
    }
} 