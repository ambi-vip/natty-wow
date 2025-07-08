package site.weixing.natty.domain.common.filestorage.service

import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.exception.*
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地文件存储服务
 * 提供高级的文件存储操作和管理功能
 */
@Service
class LocalFileStorageService {

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    }

    // 存储策略缓存
    private val strategyCache = ConcurrentHashMap<String, FileStorageStrategy>()

    /**
     * 获取或创建存储策略
     */
    private fun getOrCreateStrategy(
        provider: StorageProvider,
        config: Map<String, Any>
    ): FileStorageStrategy {
        val cacheKey = "${provider.name}_${config.hashCode()}"
        return strategyCache.computeIfAbsent(cacheKey) {
            FileStorageStrategyFactory.createStrategy(provider, config)
        }
    }

    /**
     * 上传文件
     * @param strategy 存储策略
     * @param filePath 文件路径
     * @param inputStream 文件输入流
     * @param contentType 内容类型
     * @param fileSize 文件大小
     * @return 存储信息
     */
    fun uploadFile(
        strategy: FileStorageStrategy,
        filePath: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long
    ): Mono<StorageInfo> {
        return strategy.uploadFile(filePath, inputStream, contentType, fileSize)
            .doOnSuccess { 
                logger.info("文件上传成功: {} (提供商: {})", filePath, strategy.provider) 
            }
            .doOnError { error -> 
                logger.error("文件上传失败: {} (提供商: {})", filePath, strategy.provider, error) 
            }
    }

    /**
     * 下载文件
     * @param strategy 存储策略
     * @param filePath 文件路径
     * @return 文件输入流
     */
    fun downloadFile(
        strategy: FileStorageStrategy,
        filePath: String
    ): Mono<InputStream> {
        return strategy.downloadFile(filePath)
            .doOnSuccess { 
                logger.debug("文件下载成功: {} (提供商: {})", filePath, strategy.provider) 
            }
            .doOnError { error -> 
                logger.error("文件下载失败: {} (提供商: {})", filePath, strategy.provider, error) 
            }
    }

    /**
     * 删除文件
     * @param strategy 存储策略
     * @param filePath 文件路径
     * @return 删除是否成功
     */
    fun deleteFile(
        strategy: FileStorageStrategy,
        filePath: String
    ): Mono<Boolean> {
        return strategy.deleteFile(filePath)
            .doOnSuccess { deleted -> 
                if (deleted) {
                    logger.info("文件删除成功: {} (提供商: {})", filePath, strategy.provider)
                } else {
                    logger.warn("文件不存在，删除失败: {} (提供商: {})", filePath, strategy.provider)
                }
            }
            .doOnError { error -> 
                logger.error("文件删除失败: {} (提供商: {})", filePath, strategy.provider, error) 
            }
    }

    /**
     * 批量上传文件
     * @param strategy 存储策略
     * @param files 文件信息列表
     * @return 批量上传结果
     */
    fun batchUploadFiles(
        strategy: FileStorageStrategy,
        files: List<FileUploadRequest>
    ): Mono<BatchUploadResult> {
        val successResults = mutableListOf<StorageInfo>()
        val failureResults = mutableListOf<BatchUploadFailure>()

        return Flux.fromIterable(files)
            .flatMap { request ->
                uploadFile(
                    strategy = strategy,
                    filePath = request.filePath,
                    inputStream = request.inputStream,
                    contentType = request.contentType,
                    fileSize = request.fileSize
                )
                .map<BatchUploadItem> { storageInfo -> 
                    BatchUploadItem.Success(request.filePath, storageInfo)
                }
                .onErrorResume { error ->
                    Mono.just(BatchUploadItem.Failure(request.filePath, error.message ?: "上传失败"))
                }
            }
            .collectList()
            .map { results ->
                results.forEach { item ->
                    when (item) {
                        is BatchUploadItem.Success -> successResults.add(item.storageInfo)
                        is BatchUploadItem.Failure -> failureResults.add(
                            BatchUploadFailure(item.filePath, item.errorMessage)
                        )
                    }
                }

                BatchUploadResult(
                    totalCount = files.size,
                    successCount = successResults.size,
                    failureCount = failureResults.size,
                    successResults = successResults,
                    failureResults = failureResults
                )
            }
    }

    /**
     * 批量删除文件
     * @param strategy 存储策略
     * @param filePaths 文件路径列表
     * @return 批量删除结果
     */
    fun batchDeleteFiles(
        strategy: FileStorageStrategy,
        filePaths: List<String>
    ): Mono<BatchDeleteResult> {
        val successResults = mutableListOf<String>()
        val failureResults = mutableListOf<BatchDeleteFailure>()

        return Flux.fromIterable(filePaths)
            .flatMap { filePath ->
                deleteFile(strategy, filePath)
                    .map { deleted ->
                        if (deleted) {
                            BatchDeleteItem.Success(filePath)
                        } else {
                            BatchDeleteItem.Failure(filePath, "文件不存在")
                        }
                    }
                    .onErrorResume { error ->
                        Mono.just(BatchDeleteItem.Failure(filePath, error.message ?: "删除失败"))
                    }
            }
            .collectList()
            .map { results ->
                results.forEach { item ->
                    when (item) {
                        is BatchDeleteItem.Success -> successResults.add(item.filePath)
                        is BatchDeleteItem.Failure -> failureResults.add(
                            BatchDeleteFailure(item.filePath, item.errorMessage)
                        )
                    }
                }

                BatchDeleteResult(
                    totalCount = filePaths.size,
                    successCount = successResults.size,
                    failureCount = failureResults.size,
                    successResults = successResults,
                    failureResults = failureResults
                )
            }
    }

    /**
     * 复制文件
     * @param strategy 存储策略
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 复制是否成功
     */
    fun copyFile(
        strategy: FileStorageStrategy,
        sourcePath: String,
        destPath: String,
        overwrite: Boolean = false
    ): Mono<Boolean> {
        return if (overwrite) {
            strategy.copyFile(sourcePath, destPath)
        } else {
            strategy.existsFile(destPath)
                .flatMap { exists ->
                    if (exists) {
                        Mono.error(FileAlreadyExistsException(destPath))
                    } else {
                        strategy.copyFile(sourcePath, destPath)
                    }
                }
        }
        .doOnSuccess { 
            logger.info("文件复制成功: {} -> {} (提供商: {})", sourcePath, destPath, strategy.provider) 
        }
        .doOnError { error -> 
            logger.error("文件复制失败: {} -> {} (提供商: {})", sourcePath, destPath, strategy.provider, error) 
        }
    }

    /**
     * 移动文件
     * @param strategy 存储策略
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 移动是否成功
     */
    fun moveFile(
        strategy: FileStorageStrategy,
        sourcePath: String,
        destPath: String,
        overwrite: Boolean = false
    ): Mono<Boolean> {
        return if (overwrite) {
            strategy.moveFile(sourcePath, destPath)
        } else {
            strategy.existsFile(destPath)
                .flatMap { exists ->
                    if (exists) {
                        Mono.error(FileAlreadyExistsException(destPath))
                    } else {
                        strategy.moveFile(sourcePath, destPath)
                    }
                }
        }
        .doOnSuccess { 
            logger.info("文件移动成功: {} -> {} (提供商: {})", sourcePath, destPath, strategy.provider) 
        }
        .doOnError { error -> 
            logger.error("文件移动失败: {} -> {} (提供商: {})", sourcePath, destPath, strategy.provider, error) 
        }
    }

    /**
     * 获取存储使用情况
     * @param strategy 存储策略
     * @return 存储使用情况
     */
    fun getStorageUsage(strategy: FileStorageStrategy): Mono<StorageUsage> {
        return strategy.getStorageUsage()
            .doOnSuccess { usage ->
                logger.debug(
                    "获取存储使用情况成功 (提供商: {}): 总空间={}, 已用空间={}, 文件数={}",
                    strategy.provider, usage.totalSpace, usage.usedSpace, usage.fileCount
                )
            }
    }

    /**
     * 列出目录文件
     * @param strategy 存储策略
     * @param directoryPath 目录路径
     * @param recursive 是否递归列出
     * @param filter 文件过滤器
     * @return 文件列表
     */
    fun listFiles(
        strategy: FileStorageStrategy,
        directoryPath: String,
        recursive: Boolean = false,
        filter: ((FileInfo) -> Boolean)? = null
    ): Mono<List<FileInfo>> {
        return strategy.listFiles(directoryPath, recursive)
            .map { files ->
                if (filter != null) {
                    files.filter(filter)
                } else {
                    files
                }
            }
            .doOnSuccess { files ->
                logger.debug(
                    "列出目录文件成功: {} (提供商: {}, 文件数: {})",
                    directoryPath, strategy.provider, files.size
                )
            }
    }

    /**
     * 验证文件完整性
     * @param strategy 存储策略
     * @param filePath 文件路径
     * @param expectedChecksum 期望的校验和
     * @return 验证是否通过
     */
    fun validateFileIntegrity(
        strategy: FileStorageStrategy,
        filePath: String,
        expectedChecksum: String
    ): Mono<Boolean> {
        return strategy.getFileChecksum(filePath)
            .map { actualChecksum ->
                val isValid = actualChecksum.equals(expectedChecksum, ignoreCase = true)
                if (!isValid) {
                    logger.warn(
                        "文件校验和不匹配: {} (期望: {}, 实际: {})",
                        filePath, expectedChecksum, actualChecksum
                    )
                }
                isValid
            }
            .onErrorReturn(false)
    }

    /**
     * 清理过期文件
     * @param strategy 存储策略
     * @param olderThanDays 清理多少天前的文件
     * @return 清理的文件数量
     */
    fun cleanupExpiredFiles(
        strategy: FileStorageStrategy,
        olderThanDays: Int = 7
    ): Mono<Long> {
        return strategy.cleanup(olderThanDays)
            .doOnSuccess { count ->
                logger.info(
                    "文件清理完成 (提供商: {}): 清理了 {} 个过期文件",
                    strategy.provider, count
                )
            }
    }

    /**
     * 清理存储策略缓存
     */
    fun clearStrategyCache() {
        strategyCache.clear()
        logger.info("存储策略缓存已清理")
    }
}

// 数据类定义

/**
 * 文件上传请求
 */
data class FileUploadRequest(
    val filePath: String,
    val inputStream: InputStream,
    val contentType: String,
    val fileSize: Long
)

/**
 * 批量上传结果项
 */
sealed class BatchUploadItem {
    data class Success(val filePath: String, val storageInfo: StorageInfo) : BatchUploadItem()
    data class Failure(val filePath: String, val errorMessage: String) : BatchUploadItem()
}

/**
 * 批量删除结果项
 */
sealed class BatchDeleteItem {
    data class Success(val filePath: String) : BatchDeleteItem()
    data class Failure(val filePath: String, val errorMessage: String) : BatchDeleteItem()
}

/**
 * 批量上传结果
 */
data class BatchUploadResult(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val successResults: List<StorageInfo>,
    val failureResults: List<BatchUploadFailure>
) {
    /**
     * 是否完全成功
     */
    fun isCompleteSuccess(): Boolean = failureCount == 0

    /**
     * 成功率
     */
    fun getSuccessRate(): Double = if (totalCount > 0) {
        successCount.toDouble() / totalCount.toDouble()
    } else 0.0
}

/**
 * 批量删除结果
 */
data class BatchDeleteResult(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val successResults: List<String>,
    val failureResults: List<BatchDeleteFailure>
) {
    /**
     * 是否完全成功
     */
    fun isCompleteSuccess(): Boolean = failureCount == 0

    /**
     * 成功率
     */
    fun getSuccessRate(): Double = if (totalCount > 0) {
        successCount.toDouble() / totalCount.toDouble()
    } else 0.0
}

/**
 * 批量上传失败项
 */
data class BatchUploadFailure(
    val filePath: String,
    val errorMessage: String
)

/**
 * 批量删除失败项
 */
data class BatchDeleteFailure(
    val filePath: String,
    val errorMessage: String
) 