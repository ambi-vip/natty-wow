package site.weixing.natty.domain.common.filestorage.bucket

import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.filestorage.bucket.BucketStatus
import site.weixing.natty.api.common.filestorage.bucket.IStorageBucketState
import site.weixing.natty.api.storage.bucket.BucketCapacityUpdated
import site.weixing.natty.api.storage.bucket.BucketCapacityWarning
import site.weixing.natty.api.storage.bucket.BucketPermissionsUpdated
import site.weixing.natty.api.storage.bucket.BucketStatusUpdated
import site.weixing.natty.api.storage.bucket.BucketUsageUpdated
import site.weixing.natty.api.storage.bucket.BucketValidated
import site.weixing.natty.api.storage.bucket.CapacityWarningLevel
import site.weixing.natty.api.storage.bucket.ExpiredFilesCleanedUp
import site.weixing.natty.api.storage.bucket.StorageBucketCreated
import site.weixing.natty.api.storage.bucket.StorageBucketDeleted
import site.weixing.natty.api.storage.bucket.UsageOperation
import java.time.LocalDateTime

class StorageBucketState(override val id: String) : IStorageBucketState {
    override var bucketName: String? = null
        private set
    override var ownerId: String? = null
        private set
    override var maxCapacity: Long = 0
        private set
    override var usedCapacity: Long = 0
        private set
    override var maxFileSize: Long = 0
        private set
    override var allowedTypes: Set<String> = emptySet()
        private set
    override var isPublic: Boolean = false
        private set
    override var enableCompression: Boolean = true
        private set
    override var enableEncryption: Boolean = false
        private set
    override var retentionDays: Int = 30
        private set
    override var status: BucketStatus = BucketStatus.ACTIVE
        private set
    override var description: String? = null
        private set
    
    // 统计信息
    override var fileCount: Long = 0
        private set
    override var lastAccessTime: LocalDateTime? = null
        private set
    override var lastCleanupTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onStorageBucketCreated(event: StorageBucketCreated) {
        bucketName = event.bucketName
        ownerId = event.ownerId
        maxCapacity = event.maxCapacity
        maxFileSize = event.maxFileSize
        allowedTypes = event.allowedTypes
        isPublic = event.isPublic
        enableCompression = event.enableCompression
        enableEncryption = event.enableEncryption
        retentionDays = event.retentionDays
        description = event.description
        status = BucketStatus.ACTIVE
        usedCapacity = 0
        fileCount = 0
    }

    @OnSourcing
    fun onBucketCapacityUpdated(event: BucketCapacityUpdated) {
        maxCapacity = event.newMaxCapacity
        event.newMaxFileSize?.let { maxFileSize = it }
    }

    @OnSourcing
    fun onBucketPermissionsUpdated(event: BucketPermissionsUpdated) {
        event.isPublic?.let { isPublic = it }
        event.allowedTypes?.let { allowedTypes = it }
        event.enableCompression?.let { enableCompression = it }
        event.enableEncryption?.let { enableEncryption = it }
        event.retentionDays?.let { retentionDays = it }
    }

    @OnSourcing
    fun onBucketStatusUpdated(event: BucketStatusUpdated) {
        status = event.newStatus
    }

    @OnSourcing
    fun onBucketUsageUpdated(event: BucketUsageUpdated) {
        usedCapacity = event.newUsedCapacity
        
        // 根据操作类型更新文件计数
        when (event.operation) {
            UsageOperation.ADD -> fileCount++
            UsageOperation.REMOVE -> if (fileCount > 0) fileCount--
        }
    }

    @OnSourcing
    fun onBucketValidated(event: BucketValidated) {
        lastAccessTime = LocalDateTime.now()
    }

    @OnSourcing
    fun onExpiredFilesCleanedUp(event: ExpiredFilesCleanedUp) {
        if (!event.dryRun) {
            usedCapacity = (usedCapacity - event.freedCapacity).coerceAtLeast(0)
            fileCount = (fileCount - event.cleanedFileCount).coerceAtLeast(0)
        }
        lastCleanupTime = LocalDateTime.now()
    }

    @OnSourcing
    fun onBucketCapacityWarning(event: BucketCapacityWarning) {
        // 容量警告事件，可以用于记录警告历史
        lastAccessTime = LocalDateTime.now()
    }

    @OnSourcing
    fun onStorageBucketDeleted(event: StorageBucketDeleted) {
    }
    
    /**
     * 检查存储桶是否存在
     */
    fun exists(): Boolean = bucketName != null
    
    /**
     * 检查存储桶是否可用于上传
     */
    fun isAvailableForUpload(): Boolean {
        return exists() && 
               status == BucketStatus.ACTIVE &&
               !isFull()
    }
    
    /**
     * 检查存储桶是否只读
     */
    fun isReadOnly(): Boolean {
        return status == BucketStatus.READONLY ||
               status == BucketStatus.SUSPENDED
    }
    
    /**
     * 检查是否已满
     */
    fun isFull(): Boolean {
        return usedCapacity >= maxCapacity
    }
    
    /**
     * 检查是否可以删除
     */
    fun canDelete(): Boolean {
        return exists() && fileCount == 0L && status != BucketStatus.MAINTENANCE
    }
    
    /**
     * 检查是否有足够容量
     */
    fun hasCapacity(requiredSize: Long): Boolean {
        return (maxCapacity - usedCapacity) >= requiredSize
    }
    
    /**
     * 检查文件类型是否允许
     */
    fun isFileTypeAllowed(contentType: String): Boolean {
        return allowedTypes.isEmpty() || contentType in allowedTypes
    }
    
    /**
     * 检查文件大小是否允许
     */
    fun isFileSizeAllowed(fileSize: Long): Boolean {
        return fileSize <= maxFileSize
    }
    
    /**
     * 获取使用率百分比
     */
    fun getUsagePercentage(): Double {
        return if (maxCapacity > 0) {
            (usedCapacity.toDouble() / maxCapacity.toDouble()) * 100.0
        } else {
            0.0
        }
    }
    
    /**
     * 获取可用容量
     */
    fun getAvailableCapacity(): Long {
        return maxCapacity - usedCapacity
    }
    
    /**
     * 获取容量警告级别
     */
    fun getCapacityWarningLevel(): CapacityWarningLevel? {
        val usagePercentage = getUsagePercentage()
        
        return when {
            usagePercentage >= 95.0 -> CapacityWarningLevel.CRITICAL
            usagePercentage >= 90.0 -> CapacityWarningLevel.HIGH
            usagePercentage >= 80.0 -> CapacityWarningLevel.LOW
            else -> null
        }
    }
    
    /**
     * 检查是否需要容量警告
     */
    fun needsCapacityWarning(): Boolean {
        return getCapacityWarningLevel() != null
    }
    
    /**
     * 检查是否需要清理过期文件
     */
    fun needsCleanup(): Boolean {
        val lastCleanup = lastCleanupTime ?: LocalDateTime.MIN
        val cleanupInterval = LocalDateTime.now().minusDays(1) // 每天清理一次
        
        return lastCleanup.isBefore(cleanupInterval)
    }
    
    /**
     * 检查用户是否有权限访问
     */
    fun hasAccess(userId: String?): Boolean {
        // 公开存储桶任何人都可以访问
        if (isPublic) return true
        
        // 所有者有完全访问权限
        if (userId != null && userId == ownerId) return true
        
        // 其他权限检查逻辑可以在这里扩展
        return false
    }
    
    /**
     * 检查用户是否有管理权限
     */
    fun hasManagePermission(userId: String?): Boolean {
        return userId != null && userId == ownerId
    }
    
    /**
     * 验证存储桶配置的合法性
     */
    fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxCapacity <= 0) {
            errors.add("最大容量必须大于0")
        }
        
        if (maxFileSize <= 0) {
            errors.add("单文件最大大小必须大于0")
        }
        
        if (maxFileSize > maxCapacity) {
            errors.add("单文件最大大小不能超过存储桶最大容量")
        }
        
        if (retentionDays <= 0) {
            errors.add("文件保留天数必须大于0")
        }
        
        if (usedCapacity > maxCapacity) {
            errors.add("已使用容量不能超过最大容量")
        }
        
        // 验证文件类型格式
        allowedTypes.forEach { type ->
            if (!type.matches(Regex("^[a-zA-Z0-9]+/[a-zA-Z0-9+.-]+$"))) {
                errors.add("文件类型格式不正确: $type")
            }
        }
        
        return errors
    }
    
    /**
     * 计算预计清理空间
     */
    fun getEstimatedCleanupSpace(): Long {
        // 这里简化处理，实际应该根据过期文件统计
        return (usedCapacity * 0.1).toLong() // 假设10%的文件可以清理
    }
    
    /**
     * 检查状态是否可以转换
     */
    fun canTransitionTo(newStatus: BucketStatus): Boolean {
        return when (status) {
            BucketStatus.ACTIVE -> true // 活跃状态可以转换到任何状态
            BucketStatus.MAINTENANCE -> newStatus == BucketStatus.ACTIVE || newStatus == BucketStatus.SUSPENDED
            BucketStatus.SUSPENDED -> TODO()
            BucketStatus.ARCHIVED -> TODO()
            BucketStatus.DELETED -> TODO()
            BucketStatus.READONLY -> TODO()
        }
    }
    
    /**
     * 获取建议的状态
     */
    fun getSuggestedStatus(): BucketStatus {
        return when {
            getUsagePercentage() >= 95.0 -> BucketStatus.READONLY
            else -> BucketStatus.ACTIVE
        }
    }
}