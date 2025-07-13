package site.weixing.natty.domain.common.filestorage.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.FileMetadata
import site.weixing.natty.domain.common.filestorage.file.StorageResult
import site.weixing.natty.domain.common.filestorage.processing.ProcessingOptions
import site.weixing.natty.domain.common.filestorage.processing.ProcessingCoordinator
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage

/**
 * 文件存储领域服务
 * 封装存储技术细节，提供高性能流式存储
 */
@Service
open class FileStorageService(
    private val fileStorageStrategyFactory: FileStorageStrategyFactory,
    private val processingCoordinator: ProcessingCoordinator
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    }

    /**
     * 存储文件（支持处理选项）
     */
    fun storeFile(
        path: String,
        content: Flux<DataBuffer>,
        metadata: FileMetadata,
        processingOptions: ProcessingOptions
    ): Mono<StorageResult> {
        val start = System.currentTimeMillis()
        
        return defaultStrategy()
            .flatMap { strategy ->
                if (processingOptions.requiresProcessing()) {
                    // 需要处理的文件，通过处理协调器
                    processAndStore(path, content, metadata, processingOptions, strategy)
                } else {
                    // 无需处理的文件，直接存储
                    directStore(path, content, metadata, strategy)
                }
            }
            .doOnSuccess { result ->
                val end = System.currentTimeMillis()
                logger.debug("文件存储完成: {} 耗时: {}ms", path, end - start)
            }
            .doOnError { error ->
                logger.error("文件存储失败: {} 错误: {}", path, error.message)
            }
    }

    /**
     * 处理并存储文件
     */
    private fun processAndStore(
        path: String,
        content: Flux<DataBuffer>,
        metadata: FileMetadata,
        processingOptions: ProcessingOptions,
        strategy: FileStorageStrategy
    ): Mono<StorageResult> {
        return processingCoordinator.processFile(content, processingOptions, metadata)
            .flatMap { processedResult ->
                val storageMetadata = buildStorageMetadata(metadata, processedResult.metadata)
                // 对于简化实现，直接使用原始内容进行存储
                strategy.uploadFile(path, content, metadata.contentType, storageMetadata)
                    .map { storageInfo ->
                        StorageResult(
                            storagePath = storageInfo.storagePath,
                            actualStoragePath = storageInfo.storagePath,
                            checksum = "", // 简化实现，暂不提供校验值
                            providerId = storageInfo.providerId,
                            providerName = storageInfo.provider.name
                        )
                    }
            }
    }

    /**
     * 直接存储文件（无处理）
     */
    private fun directStore(
        path: String,
        content: Flux<DataBuffer>,
        metadata: FileMetadata,
        strategy: FileStorageStrategy
    ): Mono<StorageResult> {
        val storageMetadata = buildStorageMetadata(metadata, emptyMap())
        return strategy.uploadFile(path, content, metadata.contentType, storageMetadata)
            .map { storageInfo ->
                StorageResult(
                    storagePath = storageInfo.storagePath,
                    actualStoragePath = storageInfo.storagePath,
                    checksum = "", // 直接存储时暂无校验值
                    providerId = storageInfo.providerId,
                    providerName = storageInfo.provider.name
                )
            }
    }

    /**
     * 检索文件
     */
    fun retrieveFile(path: String): Mono<Flux<DataBuffer>> {
        return defaultStrategy()
            .map { strategy ->
                strategy.downloadFile(path)
                    .doOnSubscribe { logger.debug("开始下载文件: {}", path) }
                    .doOnError { error -> logger.error("文件下载失败: {} 错误: {}", path, error.message) }
            }
    }

    /**
     * 读取文件内容流（用于处理）
     */
    fun readFile(path: String): Mono<Flux<DataBuffer>> {
        return retrieveFile(path)
    }

    /**
     * 删除文件
     */
    fun deleteFile(path: String): Mono<Boolean> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.deleteFile(path)
                    .doOnSuccess { deleted ->
                        if (deleted) logger.info("文件删除成功: {}", path)
                        else logger.warn("文件不存在: {}", path)
                    }
            }
    }

    /**
     * 移动文件
     */
    fun moveFile(sourcePath: String, targetPath: String): Mono<Boolean> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.moveFile(sourcePath, targetPath)
                    .doOnSuccess { moved ->
                        if (moved) logger.info("文件移动成功: {} -> {}", sourcePath, targetPath)
                        else logger.warn("文件移动失败: {} -> {}", sourcePath, targetPath)
                    }
                    .then(Mono.just(true))
            }
    }

    /**
     * 复制文件
     */
    fun copyFile(sourcePath: String, targetPath: String): Mono<Boolean> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.copyFile(sourcePath, targetPath)
                    .doOnSuccess { copied ->
                        if (copied) logger.info("文件复制成功: {} -> {}", sourcePath, targetPath)
                        else logger.warn("文件复制失败: {} -> {}", sourcePath, targetPath)
                    }
            }
    }

    /**
     * 获取存储使用情况
     */
    fun getStorageUsage(): Mono<StorageUsage> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.getStorageUsage()
                    .doOnSuccess { usage ->
                        logger.debug("存储使用情况: 已用 {}MB / 总共 {}MB", 
                            usage.usedSpace / 1024 / 1024, 
                            usage.totalSpace / 1024 / 1024)
                    }
            }
    }

    /**
     * 列出目录文件
     */
    fun listFiles(directoryPath: String, recursive: Boolean = false): Mono<List<FileInfo>> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.listFiles(directoryPath, recursive)
                    .doOnSuccess { files ->
                        logger.debug("目录 {} 包含 {} 个文件", directoryPath, files.size)
                    }
            }
    }

    /**
     * 清理过期文件
     */
    fun cleanup(olderThanDays: Int = 7): Mono<Long> {
        return defaultStrategy()
            .flatMap { strategy ->
                strategy.cleanup(olderThanDays)
                    .doOnSuccess { count ->
                        logger.info("清理了 {} 个超过 {} 天的文件", count, olderThanDays)
                    }
            }
    }

    /**
     * 获取默认存储策略
     */
    private fun defaultStrategy(): Mono<FileStorageStrategy> {
        // 简化实现：直接使用默认本地存储策略
        return createDefaultLocalStrategy()
            .doOnSuccess { logger.debug("使用默认本地存储策略") }
    }

    /**
     * 创建默认本地存储策略
     */
    private fun createDefaultLocalStrategy(): Mono<FileStorageStrategy> {
        return Mono.fromCallable {
            val projectRoot = System.getProperty("user.dir")
            val defaultConfig = mapOf(
                "baseDirectory" to "$projectRoot/storage/files",
                "maxFileSize" to (500 * 1024 * 1024L), // 500MB
                "enableChecksumValidation" to true
            )
            fileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL, "default", defaultConfig)
        }.onErrorMap { error ->
            RuntimeException("创建默认本地存储策略失败", error)
        }
    }

    /**
     * 构建存储元数据
     */
    private fun buildStorageMetadata(
        fileMetadata: FileMetadata,
        processingMetadata: Map<String, Any>
    ): Map<String, String> {
        return buildMap {
            // 基础文件元数据
            put("originalFileName", fileMetadata.originalFileName)
            put("uploaderId", fileMetadata.uploaderId)
            put("folderId", fileMetadata.folderId)
            put("contentType", fileMetadata.contentType)
            put("fileSize", fileMetadata.fileSize.toString())
            put("isPublic", fileMetadata.isPublic.toString())
            put("uploadTimestamp", fileMetadata.uploadTimestamp.toString())
            
            // 标签和自定义元数据
            if (fileMetadata.tags.isNotEmpty()) {
                put("tags", fileMetadata.tags.joinToString(","))
            }
            putAll(fileMetadata.customMetadata)
            
            // 处理元数据
            processingMetadata.forEach { (key, value) ->
                put("processing_$key", value.toString())
            }
        }
    }
}