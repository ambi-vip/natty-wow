package site.weixing.natty.api.common.filestorage.bucket

/**
 * 存储桶查询视图模型
 */
data class BucketView(
    val id: String,
    val bucketName: String,
    val storageId: String,
    val quotaLimit: Long,
    val usedSize: Long,
    val fileCount: Int,
    val status: BucketStatus,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * 获取剩余配额
     */
    fun getRemainingQuota(): Long {
        return if (quotaLimit <= 0) {
            Long.MAX_VALUE
        } else {
            maxOf(0, quotaLimit - usedSize)
        }
    }

    /**
     * 计算配额使用率百分比
     */
    fun getQuotaUsagePercentage(): Double {
        return if (quotaLimit <= 0) {
            0.0
        } else {
            (usedSize.toDouble() / quotaLimit.toDouble()) * 100.0
        }
    }

    /**
     * 检查是否接近配额限制
     */
    fun isNearQuotaLimit(threshold: Double = 90.0): Boolean {
        return quotaLimit > 0 && getQuotaUsagePercentage() >= threshold
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

/**
 * 存储桶统计信息
 */
data class BucketStatistics(
    val bucketName: String,
    val totalFiles: Int,
    val totalSize: Long,
    val quotaLimit: Long,
    val usagePercentage: Double,
    val averageFileSize: Long,
    val largestFileSize: Long,
    val smallestFileSize: Long,
    val lastUploadTime: Long? = null,
    val lastDeleteTime: Long? = null,
    val fileTypeDistribution: Map<String, Int> = emptyMap() // 文件类型分布
)

/**
 * 存储桶列表查询参数
 */
data class BucketListQuery(
    val storageId: String? = null,
    val status: BucketStatus? = null,
    val tags: List<String> = emptyList(),
    val namePattern: String? = null, // 支持通配符匹配
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "createdAt",
    val sortOrder: String = "desc"
)

/**
 * 存储桶配额使用情况查询
 */
data class BucketQuotaUsage(
    val bucketName: String,
    val quotaLimit: Long,
    val usedSize: Long,
    val fileCount: Int,
    val usagePercentage: Double,
    val isOverQuota: Boolean,
    val projectedFullDate: Long? = null, // 预计配额用完时间
    val growthRate: Double = 0.0 // 增长率（字节/天）
)