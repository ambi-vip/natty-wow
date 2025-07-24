package site.weixing.natty.domain.common.filestorage.bucket

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.filestorage.bucket.BucketCreated
import site.weixing.natty.api.common.filestorage.bucket.BucketDeleted
import site.weixing.natty.api.common.filestorage.bucket.BucketStatus
import site.weixing.natty.api.common.filestorage.bucket.BucketStatusChanged
import site.weixing.natty.api.common.filestorage.bucket.BucketUpdated
import site.weixing.natty.api.common.filestorage.bucket.FileUsageAdded
import site.weixing.natty.api.common.filestorage.bucket.FileUsageRemoved

/**
 * 存储桶状态类
 * 管理存储桶的完整生命周期状态
 */
class BucketState(override val id: String) : Identifier {

    var bucketName: String? = null
        private set

    var storageId: String? = null
        private set

    var quotaLimit: Long = 0
        private set

    var usedSize: Long = 0
        private set

    var fileCount: Int = 0
        private set

    var status: BucketStatus = BucketStatus.ACTIVE
        private set

    var description: String? = null
        private set

    var tags: List<String> = emptyList()
        private set

    var createdAt: Long = System.currentTimeMillis()
        private set

    var updatedAt: Long = System.currentTimeMillis()
        private set

    @OnSourcing
    fun onBucketCreated(event: BucketCreated) {
        this.bucketName = event.bucketName
        this.storageId = event.storageId
        this.quotaLimit = event.quotaLimit
        this.description = event.description
        this.tags = event.tags
        this.status = BucketStatus.ACTIVE
        this.usedSize = 0
        this.fileCount = 0
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onBucketUpdated(event: BucketUpdated) {
        event.quotaLimit?.let { this.quotaLimit = it }
        event.description?.let { this.description = it }
        event.tags?.let { this.tags = it }
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onBucketDeleted(event: BucketDeleted) {
        this.status = BucketStatus.DELETED
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onBucketStatusChanged(event: BucketStatusChanged) {
        this.status = event.newStatus
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileUsageAdded(event: FileUsageAdded) {
        this.usedSize = event.newUsedSize
        this.fileCount = event.newFileCount
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileUsageRemoved(event: FileUsageRemoved) {
        this.usedSize = event.newUsedSize
        this.fileCount = event.newFileCount
        this.updatedAt = System.currentTimeMillis()
    }

    /**
     * 获取剩余配额
     */
    fun getRemainingQuota(): Long {
        return if (quotaLimit <= 0) {
            Long.MAX_VALUE // 无限制
        } else {
            maxOf(0, quotaLimit - usedSize)
        }
    }

    /**
     * 计算配额使用率百分比
     */
    fun getQuotaUsagePercentage(): Double {
        return if (quotaLimit <= 0) {
            0.0 // 无限制时返回0%
        } else {
            (usedSize.toDouble() / quotaLimit.toDouble()) * 100.0
        }
    }

    /**
     * 检查是否有足够的配额空间
     */
    fun hasEnoughQuota(requiredSize: Long): Boolean {
        return quotaLimit <= 0 || (usedSize + requiredSize) <= quotaLimit
    }

    /**
     * 检查是否接近配额限制
     */
    fun isNearQuotaLimit(threshold: Double = 90.0): Boolean {
        return quotaLimit > 0 && getQuotaUsagePercentage() >= threshold
    }

    /**
     * 检查存储桶是否可用
     */
    fun isAvailable(): Boolean {
        return status == BucketStatus.ACTIVE
    }

    /**
     * 检查存储桶是否已删除
     */
    fun isDeleted(): Boolean {
        return status == BucketStatus.DELETED
    }

    /**
     * 检查存储桶是否为空
     */
    fun isEmpty(): Boolean {
        return fileCount == 0 && usedSize == 0.toLong()
    }

    /**
     * 格式化配额信息
     */
    fun getQuotaInfo(): String {
        return if (quotaLimit <= 0) {
            "无限制 (已使用: ${formatFileSize(usedSize)})"
        } else {
            "${formatFileSize(usedSize)} / ${formatFileSize(quotaLimit)} (${String.format("%.1f", getQuotaUsagePercentage())}%)"
        }
    }

    /**
     * 格式化文件大小为可读格式
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }
}