package site.weixing.natty.api.common.filestorage.bucket

/**
 * 存储桶创建事件
 */
data class BucketCreated(
    val bucketName: String,
    val storageId: String,
    val quotaLimit: Long,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 存储桶更新事件
 */
data class BucketUpdated(
    val quotaLimit: Long? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 存储桶删除事件
 */
data class BucketDeleted(
    val bucketName: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * 存储桶状态变更事件
 */
data class BucketStatusChanged(
    val oldStatus: BucketStatus,
    val newStatus: BucketStatus,
    val changedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * 文件使用量增加事件
 */
data class FileUsageAdded(
    val fileId: String,
    val fileSize: Long,
    val newUsedSize: Long,
    val newFileCount: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * 文件使用量减少事件
 */
data class FileUsageRemoved(
    val fileId: String,
    val fileSize: Long,
    val newUsedSize: Long,
    val newFileCount: Int,
    val removedAt: Long = System.currentTimeMillis()
)

/**
 * 存储桶配额警告事件（当使用量接近配额限制时触发）
 */
data class BucketQuotaWarning(
    val bucketName: String,
    val quotaLimit: Long,
    val usedSize: Long,
    val usagePercentage: Double,
    val remainingQuota: Long,
    val warningThreshold: Double = 90.0,
    val warningAt: Long = System.currentTimeMillis()
)

/**
 * 存储桶配额耗尽事件（当达到配额限制时触发）
 */
data class BucketQuotaExhausted(
    val bucketName: String,
    val quotaLimit: Long,
    val usedSize: Long,
    val exhaustedAt: Long = System.currentTimeMillis()
)

/**
 * 存储桶统计更新事件（定期统计信息更新）
 */
data class BucketStatisticsUpdated(
    val bucketName: String,
    val fileCount: Int,
    val usedSize: Long,
    val quotaLimit: Long,
    val usagePercentage: Double,
    val lastFileUploadAt: Long? = null,
    val lastFileDeleteAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)