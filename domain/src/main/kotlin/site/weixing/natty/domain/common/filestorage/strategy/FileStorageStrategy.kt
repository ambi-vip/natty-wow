package site.weixing.natty.domain.common.filestorage.strategy

import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import org.springframework.core.io.buffer.DataBuffer

/**
 * 文件存储策略接口
 * 定义所有存储提供商的统一操作接口
 */
interface FileStorageStrategy {
    /**
     * 存储提供商类型
     */
    val provider: StorageProvider

    /**
     * 上传文件（流式）
     * @param filePath 文件存储路径
     * @param dataBufferFlux 文件内容流
     * @param contentType 文件内容类型
     * @param metadata 文件元数据
     * @return 存储信息
     */
    fun uploadFile(
        filePath: String,
        dataBufferFlux: Flux<DataBuffer>,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): Mono<StorageInfo>

    /**
     * 下载文件（流式）
     * @param filePath 文件存储路径
     * @return 文件内容流
     */
    fun downloadFile(filePath: String): Flux<DataBuffer>

    fun deleteFile(filePath: String): Mono<Boolean>
    fun existsFile(filePath: String): Mono<Boolean>
    fun getFileSize(filePath: String): Mono<Long>
    fun getFileUrl(filePath: String, expirationTimeInSeconds: Long? = null): Mono<String>
    fun copyFile(sourcePath: String, destPath: String): Mono<Boolean>
    fun moveFile(sourcePath: String, destPath: String): Mono<Boolean>
    fun listFiles(directoryPath: String, recursive: Boolean = false): Mono<List<FileInfo>>
    fun createDirectory(directoryPath: String): Mono<Boolean>
    fun deleteDirectory(directoryPath: String, recursive: Boolean = false): Mono<Boolean>
    fun getStorageUsage(): Mono<StorageUsage>
    fun validateConfig(config: Map<String, Any>): Mono<Boolean>
    fun getFileChecksum(filePath: String): Mono<String>
    fun cleanup(olderThanDays: Int = 7): Mono<Long>
    fun isAvailable(): Mono<Boolean> = Mono.just(true)
}

/**
 * 文件信息数据类
 */
data class FileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val contentType: String? = null,
    val etag: String? = null
)

/**
 * 存储使用情况数据类
 */
data class StorageUsage(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val fileCount: Long
) {
    /**
     * 计算使用率百分比
     */
    fun getUsagePercentage(): Double {
        return if (totalSpace > 0) {
            (usedSpace.toDouble() / totalSpace.toDouble()) * 100
        } else 0.0
    }
    /**
     * 剩余空间百分比
     */
    fun getFreePercentage(): Double {
        return 100.0 - getUsagePercentage()
    }
} 