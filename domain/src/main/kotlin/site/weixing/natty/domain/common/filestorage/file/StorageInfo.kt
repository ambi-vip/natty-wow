package site.weixing.natty.domain.common.filestorage.file

import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import java.time.LocalDateTime

/**
 * 存储信息值对象
 * 包含文件在存储系统中的详细信息
 */
data class StorageInfo(
    val provider: StorageProvider,
    val providerId: String,
    val storagePath: String,
    val etag: String? = null,
    val bucket: String? = null,
    val region: String? = null,
    val endpoint: String? = null,
    val accessUrl: String? = null,
    val cdnUrl: String? = null,
    val compressionType: String? = null,
    val encryptionKey: String? = null,
    val storageClass: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * 创建本地存储信息
         */
        fun local(providerId: String, storagePath: String, etag: String? = null): StorageInfo {
            return StorageInfo(
                providerId = providerId,
                provider = StorageProvider.LOCAL,
                storagePath = storagePath,
                etag = etag
            )
        }
        
        /**
         * 创建S3存储信息
         */
        fun s3(
            providerId: String,
            storagePath: String,
            bucket: String,
            region: String,
            etag: String? = null,
            endpoint: String? = null
        ): StorageInfo {
            return StorageInfo(
                providerId = providerId,
                provider = StorageProvider.S3,
                storagePath = storagePath,
                bucket = bucket,
                region = region,
                etag = etag,
                endpoint = endpoint
            )
        }
        
        /**
         * 创建阿里云OSS存储信息
         */
        fun aLiYunOss(
            providerId: String,
            storagePath: String,
            bucket: String,
            endpoint: String,
            etag: String? = null
        ): StorageInfo {
            return StorageInfo(
                providerId = providerId,
                provider = StorageProvider.ALIYUN_OSS,
                storagePath = storagePath,
                bucket = bucket,
                endpoint = endpoint,
                etag = etag
            )
        }
    }
    
    /**
     * 获取完整的访问URL
     */
    fun fullUrl(): String? {
        return cdnUrl ?: accessUrl
    }
    
    /**
     * 是否启用了CDN
     */
    fun hasCdn(): Boolean {
        return !cdnUrl.isNullOrBlank()
    }
    
    /**
     * 是否为本地存储
     */
    fun isLocal(): Boolean {
        return provider == StorageProvider.LOCAL
    }
    
    /**
     * 是否为云存储
     */
    fun isCloud(): Boolean {
        return provider == StorageProvider.S3 || provider == StorageProvider.ALIYUN_OSS
    }
} 