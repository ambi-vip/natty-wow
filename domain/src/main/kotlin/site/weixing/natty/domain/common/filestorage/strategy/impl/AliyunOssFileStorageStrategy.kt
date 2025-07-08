package site.weixing.natty.domain.common.filestorage.strategy.impl

import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import site.weixing.natty.domain.common.filestorage.exception.StorageProviderUnavailableException
import java.io.InputStream

/**
 * 阿里云OSS存储策略空实现
 * TODO: 实现完整的阿里云OSS存储功能
 */
class AliyunOssFileStorageStrategy(
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val endpoint: String,
    private val bucketName: String,
    private val region: String? = null
) : FileStorageStrategy {

    override val provider: StorageProvider = StorageProvider.ALIYUN_OSS

    override fun uploadFile(
        filePath: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long
    ): Mono<StorageInfo> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun downloadFile(filePath: String): Mono<InputStream> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun deleteFile(filePath: String): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun existsFile(filePath: String): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun getFileSize(filePath: String): Mono<Long> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?): Mono<String> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun listFiles(directoryPath: String, recursive: Boolean): Mono<List<FileInfo>> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun createDirectory(directoryPath: String): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun deleteDirectory(directoryPath: String, recursive: Boolean): Mono<Boolean> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun getStorageUsage(): Mono<StorageUsage> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun validateConfig(config: Map<String, Any>): Mono<Boolean> {
        return Mono.just(true) // 基本配置验证
    }

    override fun getFileChecksum(filePath: String): Mono<String> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }

    override fun cleanup(olderThanDays: Int): Mono<Long> {
        return Mono.error(StorageProviderUnavailableException(
            provider = "ALIYUN_OSS",
            reason = "阿里云OSS存储策略尚未实现"
        ))
    }
} 