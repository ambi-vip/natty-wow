package site.weixing.natty.domain.common.filestorage.temp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import reactor.util.function.Tuples

/**
 * 临时文件事务性操作管理器
 * 
 * 提供事务性的临时文件操作，确保操作失败时能够自动清理相关资源。
 * 实现了"要么全部成功，要么完全回滚"的事务语义。
 * 
 * 主要特性：
 * 1. 自动资源清理 - 操作失败时自动清理临时文件
 * 2. 异常安全 - 即使清理过程中出现异常也不会影响原始异常的传播
 * 3. 批量操作支持 - 支持多个临时文件的事务性操作
 * 4. 灵活的回滚策略 - 支持自定义清理逻辑
 */
@Component
class TemporaryFileTransaction(
    private val temporaryFileManager: TemporaryFileManager
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 执行单个临时文件的事务性操作
     * 
     * 在操作完成后（无论成功或失败）自动清理指定的临时文件。
     * 如果操作成功，返回结果；如果操作失败，清理临时文件后重新抛出异常。
     * 
     * @param temporaryRef 临时文件引用ID
     * @param operation 要执行的操作
     * @return 操作结果的 Mono 包装
     */
    fun <T> executeWithCleanup(
        temporaryRef: String,
        operation: () -> Mono<T>
    ): Mono<T> {
        logger.debug { "开始事务性操作: 临时文件引用=$temporaryRef" }
        
        return operation()
            .doOnSuccess { result ->
                logger.debug { "操作成功完成: 临时文件引用=$temporaryRef" }
            }
            .doOnError { error ->
                logger.warn(error) { "操作失败: 临时文件引用=$temporaryRef, 错误=${error.message}" }
            }
            .publishOn(Schedulers.boundedElastic())
            .doFinally { signalType ->
                // 无论成功失败都清理临时文件
                logger.debug { "开始清理临时文件: 引用=$temporaryRef, 信号类型=$signalType" }
                
                temporaryFileManager.deleteTemporaryFile(temporaryRef)
                    .doOnSuccess { deleted ->
                        if (deleted) {
                            logger.debug { "临时文件清理成功: $temporaryRef" }
                        } else {
                            logger.warn { "临时文件不存在或已被清理: $temporaryRef" }
                        }
                    }
                    .doOnError { cleanupError ->
                        logger.error(cleanupError) { "临时文件清理失败: $temporaryRef" }
                    }
                    .onErrorResume { cleanupError ->
                        // 清理失败不应该影响原始操作的结果
                        logger.warn(cleanupError) { "忽略清理错误，避免影响原始操作: $temporaryRef" }
                        Mono.just(false)
                    }
                    .subscribe()
            }
    }
    
    /**
     * 执行多个临时文件的事务性操作
     * 
     * 在操作完成后自动清理所有指定的临时文件。
     * 适用于需要处理多个临时文件的复杂操作场景。
     * 
     * @param temporaryRefs 临时文件引用ID列表
     * @param operation 要执行的操作
     * @return 操作结果的 Mono 包装
     */
    fun <T> executeWithBatchCleanup(
        temporaryRefs: List<String>,
        operation: () -> Mono<T>
    ): Mono<T> {
        logger.debug { "开始批量事务性操作: 临时文件数量=${temporaryRefs.size}" }
        
        return operation()
            .doOnSuccess { result ->
                logger.debug { "批量操作成功完成: 涉及${temporaryRefs.size}个临时文件" }
            }
            .doOnError { error ->
                logger.warn(error) { "批量操作失败: 涉及${temporaryRefs.size}个临时文件, 错误=${error.message}" }
            }
            .doFinally { signalType ->
                logger.debug { "开始批量清理临时文件: 数量=${temporaryRefs.size}, 信号类型=$signalType" }
                
                // 并行清理所有临时文件
                val cleanupOperations = temporaryRefs.map { ref ->
                    temporaryFileManager.deleteTemporaryFile(ref)
                        .doOnSuccess { deleted ->
                            if (deleted) {
                                logger.debug { "临时文件清理成功: $ref" }
                            } else {
                                logger.warn { "临时文件不存在或已被清理: $ref" }
                            }
                        }
                        .doOnError { cleanupError ->
                            logger.error(cleanupError) { "临时文件清理失败: $ref" }
                        }
                        .onErrorReturn(false) // 转换错误为默认值，避免影响其他清理操作
                }
                
                // 等待所有清理操作完成
                Mono.zip(cleanupOperations) { results ->
                    val successCount = results.count { it == true }
                    val totalCount = results.size
                    logger.info { "批量清理完成: 成功清理 $successCount/$totalCount 个临时文件" }
                    successCount
                }
                .subscribe()
            }
    }
    
    /**
     * 创建临时文件并执行事务性操作
     * 
     * 组合了临时文件创建和事务性操作的便捷方法。
     * 首先创建临时文件，然后执行操作，最后自动清理。
     * 
     * @param createFileOperation 创建临时文件的操作
     * @param businessOperation 业务操作（接收临时文件引用作为参数）
     * @return 操作结果的 Mono 包装，包含临时文件引用和业务操作结果
     */
    fun <T : Any> createAndExecute(
        createFileOperation: () -> Mono<TemporaryFileReference>,
        businessOperation: (String) -> Mono<T>
    ): Mono<Tuple2<TemporaryFileReference, T>> {
        logger.debug { "开始创建并执行事务性操作" }
        
        return createFileOperation()
            .doOnSuccess { reference ->
                logger.debug { "临时文件创建成功: ${reference.referenceId}" }
            }
            .flatMap { reference ->
                val businessMono: Mono<Tuple2<TemporaryFileReference, T>> = businessOperation(reference.referenceId)
                    .map { result -> Tuples.of<TemporaryFileReference, T>(reference, result) }
                
                // 使用单文件事务包装业务操作
                executeWithCleanup(reference.referenceId) { businessMono }
            }
    }
    
    /**
     * 执行带有自定义清理逻辑的事务性操作
     * 
     * 允许用户提供自定义的清理逻辑，而不仅仅是删除临时文件。
     * 适用于需要特殊清理处理的复杂场景。
     * 
     * @param operation 要执行的操作
     * @param customCleanup 自定义清理逻辑
     * @return 操作结果的 Mono 包装
     */
    fun <T> executeWithCustomCleanup(
        operation: () -> Mono<T>,
        customCleanup: () -> Mono<Void>
    ): Mono<T> {
        logger.debug { "开始自定义清理事务性操作" }
        
        return operation()
            .doOnSuccess { result: T ->
                logger.debug { "自定义清理操作成功完成" }
            }
            .doOnError { error: Throwable ->
                logger.warn(error) { "自定义清理操作失败: ${error.message}" }
            }
            .doFinally { signalType ->
                logger.debug { "开始执行自定义清理逻辑: 信号类型=$signalType" }
                
                customCleanup()
                    .doOnSuccess { _: Void? ->
                        logger.debug { "自定义清理逻辑执行成功" }
                    }
                    .doOnError { cleanupError: Throwable ->
                        logger.error(cleanupError) { "自定义清理逻辑执行失败" }
                    }
                    .onErrorResume { cleanupError: Throwable ->
                        // 清理失败不应该影响原始操作的结果
                        logger.warn(cleanupError) { "忽略自定义清理错误，避免影响原始操作" }
                        Mono.empty<Void>()
                    }
                    .subscribe()
            }
    }
    
    /**
     * 检查临时文件是否有效，无效则自动清理
     * 
     * 用于验证临时文件的有效性，如果文件已过期或不存在，则自动清理相关资源。
     * 
     * @param temporaryRef 临时文件引用ID
     * @return 文件是否有效的 Mono 包装
     */
    fun validateAndCleanupIfNeeded(temporaryRef: String): Mono<Boolean> {
        return temporaryFileManager.isTemporaryFileValid(temporaryRef)
            .flatMap { isValid ->
                if (!isValid) {
                    logger.info { "临时文件无效，开始清理: $temporaryRef" }
                    temporaryFileManager.deleteTemporaryFile(temporaryRef)
                        .map { false } // 文件无效，返回 false
                } else {
                    Mono.just(true) // 文件有效，返回 true
                }
            }
            .doOnError { error ->
                logger.error(error) { "验证临时文件时出错: $temporaryRef" }
            }
            .onErrorReturn(false) // 发生错误时认为文件无效
    }
} 