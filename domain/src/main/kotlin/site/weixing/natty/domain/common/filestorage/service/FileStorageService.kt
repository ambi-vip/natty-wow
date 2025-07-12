package site.weixing.natty.domain.common.filestorage.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import kotlin.jvm.java

@Service
class FileStorageService(
    private val fileStorageStrategyFactory: FileStorageStrategyFactory
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    }

    // 默认本地存储策略
    fun defaultStrategy(): FileStorageStrategy {
        val projectRoot = System.getProperty("user.dir")
        val defaultConfig = mapOf(
            "baseDirectory" to "$projectRoot/storage/files",
            "maxFileSize" to (100 * 1024 * 1024L),
            "enableChecksumValidation" to true
        )
        return fileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL, defaultConfig)
    }

    fun uploadFile(
        filePath: String,
        dataBufferFlux: Flux<DataBuffer>,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): Mono<StorageInfo> {
        val strategy = defaultStrategy()
        return strategy.uploadFile(filePath, dataBufferFlux, contentType, metadata)
            .doOnSuccess { logger.info("文件流式上传成功: {} (提供商: {})", filePath, strategy.provider) }
            .doOnError { error -> logger.error("文件流式上传失败: {} (提供商: {})", filePath, strategy.provider, error) }
    }

    fun downloadFile(filePath: String): Flux<DataBuffer> {
        val strategy = defaultStrategy()
        return strategy.downloadFile(filePath)
            .doOnSubscribe { logger.debug("开始流式下载文件: {} (提供商: {})", filePath, strategy.provider) }
            .doOnError { error -> logger.error("文件流式下载失败: {} (提供商: {})", filePath, strategy.provider, error) }
    }

    fun deleteFile(filePath: String): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.deleteFile(filePath)
            .doOnSuccess { deleted ->
                if (deleted) logger.info("文件删除成功: {} (提供商: {})", filePath, strategy.provider)
                else logger.warn("文件不存在，删除失败: {} (提供商: {})", filePath, strategy.provider)
            }
            .doOnError { error -> logger.error("文件删除失败: {} (提供商: {})", filePath, strategy.provider, error) }
    }

    fun existsFile(filePath: String): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.existsFile(filePath)
    }

    fun getFileSize(filePath: String): Mono<Long> {
        val strategy = defaultStrategy()
        return strategy.getFileSize(filePath)
    }

    fun getFileUrl(filePath: String, expirationTimeInSeconds: Long? = null): Mono<String> {
        val strategy = defaultStrategy()
        return strategy.getFileUrl(filePath, expirationTimeInSeconds)
    }

    fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.copyFile(sourcePath, destPath)
    }

    fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.moveFile(sourcePath, destPath)
    }

    fun listFiles(directoryPath: String, recursive: Boolean = false): Mono<List<FileInfo>> {
        val strategy = defaultStrategy()
        return strategy.listFiles(directoryPath, recursive)
    }

    fun createDirectory(directoryPath: String): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.createDirectory(directoryPath)
    }

    fun deleteDirectory(directoryPath: String, recursive: Boolean = false): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.deleteDirectory(directoryPath, recursive)
    }

    fun getStorageUsage(): Mono<StorageUsage> {
        val strategy = defaultStrategy()
        return strategy.getStorageUsage()
    }

    fun validateConfig(config: Map<String, Any>): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.validateConfig(config)
    }

    fun getFileChecksum(filePath: String): Mono<String> {
        val strategy = defaultStrategy()
        return strategy.getFileChecksum(filePath)
    }

    fun cleanup(olderThanDays: Int = 7): Mono<Long> {
        val strategy = defaultStrategy()
        return strategy.cleanup(olderThanDays)
    }

    fun isAvailable(): Mono<Boolean> {
        val strategy = defaultStrategy()
        return strategy.isAvailable()
    }
} 