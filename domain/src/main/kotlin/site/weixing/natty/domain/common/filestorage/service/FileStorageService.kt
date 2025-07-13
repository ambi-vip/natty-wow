package site.weixing.natty.domain.common.filestorage.service

import me.ahoo.wow.apiclient.query.switchNotFoundToEmpty
import me.ahoo.wow.exception.throwNotFoundIfEmpty
import me.ahoo.wow.query.dsl.listQuery
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.storage.StorageConfigState
import site.weixing.natty.domain.common.filestorage.storage.StorageConfigStateProperties
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import kotlin.jvm.java

@Service
class FileStorageService(
    private val fileStorageStrategyFactory: FileStorageStrategyFactory,
    private val storageConfigQueryService: SnapshotQueryService<StorageConfigState>,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    }

    /**
     * 获取可用的存储策略映射
     * 基于真实的存储配置查询已启用的存储策略
     */
    fun defaultStrategy(): Mono<FileStorageStrategy> {
        return singleQuery {
            condition {
                nestedState()
                StorageConfigStateProperties.IS_DEFAULT.eq(true)
            }
        }.query(storageConfigQueryService)
            .toState()
            .flatMap { snapshot ->
                logger.debug("使用配置 ${snapshot.name} 进行存储")
                fileStorageStrategyFactory.createStrategy(snapshot.provider,snapshot.id, snapshot.config).toMono()
            }
            .switchIfEmpty {
                logger.warn("所有配置的存储策略创建失败，回退到默认本地存储")
                createDefaultLocalStrategy()
            }
    }

    /**
     * 创建默认本地存储策略
     */
    fun createDefaultLocalStrategy(): Mono<FileStorageStrategy> {
        return Mono.fromCallable {
            val projectRoot = System.getProperty("user.dir")
            val defaultConfig = mapOf(
                "baseDirectory" to "$projectRoot/storage/files",
                "maxFileSize" to (100 * 1024 * 1024L),
                "enableChecksumValidation" to true
            )
            fileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL,"1", defaultConfig)
        }.onErrorMap { error ->
                RuntimeException("创建默认本地存储策略失败", error)
            }
    }

    fun uploadFile(
        filePath: String,
        dataBufferFlux: Flux<DataBuffer>,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): Mono<StorageInfo> {
        return defaultStrategy().flatMap { strategy ->
            strategy.uploadFile(filePath, dataBufferFlux, contentType, metadata)
                .doOnSuccess { logger.info("文件流式上传成功: {} (提供商: {})", filePath, strategy.provider) }
                .doOnError { error -> logger.error("文件流式上传失败: {} (提供商: {})", filePath, strategy.provider, error) }
        }
    }

    fun downloadFile(filePath: String): Flux<DataBuffer> {
        return defaultStrategy().flatMapMany { strategy ->
            strategy.downloadFile(filePath)
                .doOnSubscribe { logger.debug("开始流式下载文件: {} (提供商: {})", filePath, strategy.provider) }
                .doOnError { error -> logger.error("文件流式下载失败: {} (提供商: {})", filePath, strategy.provider, error) }
        }
    }

    fun deleteFile(filePath: String): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.deleteFile(filePath)
                .doOnSuccess { deleted ->
                    if (deleted) logger.info("文件删除成功: {} (提供商: {})", filePath, strategy.provider)
                    else logger.warn("文件不存在，删除失败: {} (提供商: {})", filePath, strategy.provider)
                }
                .doOnError { error -> logger.error("文件删除失败: {} (提供商: {})", filePath, strategy.provider, error) }
        }
    }

    fun existsFile(filePath: String): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.existsFile(filePath)
        }
    }

    fun getFileSize(filePath: String): Mono<Long> {
        return defaultStrategy().flatMap { strategy ->
            strategy.getFileSize(filePath)
        }
    }

    fun getFileUrl(filePath: String, expirationTimeInSeconds: Long? = null): Mono<String> {
        return defaultStrategy().flatMap { strategy ->
            strategy.getFileUrl(filePath, expirationTimeInSeconds)
        }
    }

    fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.copyFile(sourcePath, destPath)
        }
    }

    fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.moveFile(sourcePath, destPath)
        }
    }

    fun listFiles(directoryPath: String, recursive: Boolean = false): Mono<List<FileInfo>> {
        return defaultStrategy().flatMap { strategy ->
            strategy.listFiles(directoryPath, recursive)
        }
    }

    fun createDirectory(directoryPath: String): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.createDirectory(directoryPath)
        }
    }

    fun deleteDirectory(directoryPath: String, recursive: Boolean = false): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.deleteDirectory(directoryPath, recursive)
        }
    }

    fun getStorageUsage(): Mono<StorageUsage> {
        return defaultStrategy().flatMap { strategy ->
            strategy.getStorageUsage()
        }
    }

    fun validateConfig(config: Map<String, Any>): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.validateConfig(config)
        }
    }

    fun getFileChecksum(filePath: String): Mono<String> {
        return defaultStrategy().flatMap { strategy ->
            strategy.getFileChecksum(filePath)
        }
    }

    fun cleanup(olderThanDays: Int = 7): Mono<Long> {
        return defaultStrategy().flatMap { strategy ->
            strategy.cleanup(olderThanDays)
        }
    }

    fun isAvailable(): Mono<Boolean> {
        return defaultStrategy().flatMap { strategy ->
            strategy.isAvailable()
        }
    }
} 