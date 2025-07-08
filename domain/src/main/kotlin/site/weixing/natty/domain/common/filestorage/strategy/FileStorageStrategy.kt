package site.weixing.natty.domain.common.filestorage.strategy

import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import reactor.core.publisher.Mono
import java.io.InputStream

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
     * 上传文件
     * @param filePath 文件存储路径
     * @param inputStream 文件输入流
     * @param contentType 文件内容类型
     * @param fileSize 文件大小
     * @return 存储信息
     */
    fun uploadFile(
        filePath: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long
    ): Mono<StorageInfo>
    
    /**
     * 下载文件
     * @param filePath 文件存储路径
     * @return 文件输入流
     */
    fun downloadFile(filePath: String): Mono<InputStream>
    
    /**
     * 删除文件
     * @param filePath 文件存储路径
     * @return 删除是否成功
     */
    fun deleteFile(filePath: String): Mono<Boolean>
    
    /**
     * 检查文件是否存在
     * @param filePath 文件存储路径
     * @return 文件是否存在
     */
    fun existsFile(filePath: String): Mono<Boolean>
    
    /**
     * 获取文件大小
     * @param filePath 文件存储路径
     * @return 文件大小（字节）
     */
    fun getFileSize(filePath: String): Mono<Long>
    
    /**
     * 获取文件的访问URL
     * @param filePath 文件存储路径
     * @param expirationTimeInSeconds 过期时间（秒），null表示永不过期
     * @return 文件访问URL
     */
    fun getFileUrl(
        filePath: String,
        expirationTimeInSeconds: Long? = null
    ): Mono<String>
    
    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 复制是否成功
     */
    fun copyFile(
        sourcePath: String,
        destPath: String
    ): Mono<Boolean>
    
    /**
     * 移动文件
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 移动是否成功
     */
    fun moveFile(
        sourcePath: String,
        destPath: String
    ): Mono<Boolean>
    
    /**
     * 列出目录下的文件
     * @param directoryPath 目录路径
     * @param recursive 是否递归查找
     * @return 文件列表
     */
    fun listFiles(
        directoryPath: String,
        recursive: Boolean = false
    ): Mono<List<FileInfo>>
    
    /**
     * 创建目录
     * @param directoryPath 目录路径
     * @return 创建是否成功
     */
    fun createDirectory(directoryPath: String): Mono<Boolean>
    
    /**
     * 删除目录
     * @param directoryPath 目录路径
     * @param recursive 是否递归删除
     * @return 删除是否成功
     */
    fun deleteDirectory(
        directoryPath: String,
        recursive: Boolean = false
    ): Mono<Boolean>
    
    /**
     * 获取存储使用情况
     * @return 存储使用信息
     */
    fun getStorageUsage(): Mono<StorageUsage>
    
    /**
     * 验证存储配置
     * @param config 存储配置参数
     * @return 验证是否成功
     */
    fun validateConfig(config: Map<String, Any>): Mono<Boolean>
    
    /**
     * 获取文件的校验和（MD5或SHA256）
     * @param filePath 文件存储路径
     * @return 文件校验和
     */
    fun getFileChecksum(filePath: String): Mono<String>
    
    /**
     * 清理过期或临时文件
     * @param olderThanDays 清理多少天前的文件
     * @return 清理的文件数量
     */
    fun cleanup(olderThanDays: Int = 7): Mono<Long>
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