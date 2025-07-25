package site.weixing.natty.domain.common.filestorage.bucket

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.bucket.BucketStatus
import site.weixing.natty.api.storage.bucket.BucketCapacityUpdated
import site.weixing.natty.api.storage.bucket.BucketCapacityWarning
import site.weixing.natty.api.storage.bucket.BucketPermissionsUpdated
import site.weixing.natty.api.storage.bucket.BucketStatusUpdated
import site.weixing.natty.api.storage.bucket.BucketUsageUpdated
import site.weixing.natty.api.storage.bucket.BucketValidated
import site.weixing.natty.api.storage.bucket.BucketValidationFailed
import site.weixing.natty.api.storage.bucket.CleanupExpiredFiles
import site.weixing.natty.api.storage.bucket.CreateStorageBucket
import site.weixing.natty.api.storage.bucket.DeleteStorageBucket
import site.weixing.natty.api.storage.bucket.ExpiredFilesCleanedUp
import site.weixing.natty.api.storage.bucket.StorageBucketCreated
import site.weixing.natty.api.storage.bucket.StorageBucketDeleted
import site.weixing.natty.api.storage.bucket.UpdateBucketCapacity
import site.weixing.natty.api.storage.bucket.UpdateBucketPermissions
import site.weixing.natty.api.storage.bucket.UpdateBucketStatus
import site.weixing.natty.api.storage.bucket.UpdateBucketUsage
import site.weixing.natty.api.storage.bucket.ValidateBucket
import java.time.LocalDateTime

/**
 * 存储桶聚合根
 *
 * 负责处理存储桶相关的所有业务命令，包括创建、容量管理、权限控制等操作。
 * 实现完整的业务规则验证和状态管理。
 */
@AggregateRoot
@StaticTenantId
class StorageBucket(private val state: StorageBucketState) {

    companion object {
        private val logger = LoggerFactory.getLogger(StorageBucket::class.java)
    }

    @OnCommand
    fun onCreate(command: CreateStorageBucket): StorageBucketCreated {
        logger.info("创建存储桶: {}", command.bucketName)

        // 验证存储桶不存在
        require(!state.exists()) { "存储桶已存在" }

        // 验证存储桶名称唯一性(这里简化处理，实际应该通过域服务验证)
        validateBucketName(command.bucketName)

        // 验证配置参数
        validateBucketConfiguration(
            maxCapacity = command.maxCapacity,
            maxFileSize = command.maxFileSize,
            retentionDays = command.retentionDays,
            allowedTypes = command.allowedTypes
        )

        return StorageBucketCreated(
            bucketName = command.bucketName,
            ownerId = command.ownerId,
            maxCapacity = command.maxCapacity,
            maxFileSize = command.maxFileSize,
            allowedTypes = command.allowedTypes,
            isPublic = command.isPublic,
            enableCompression = command.enableCompression,
            enableEncryption = command.enableEncryption,
            retentionDays = command.retentionDays,
            description = command.description
        )
    }

    @OnCommand
    fun onUpdateCapacity(command: UpdateBucketCapacity): BucketCapacityUpdated {
        logger.info("更新存储桶容量: {}", command.id)

        // 验证存储桶存在
        require(state.exists()) { "存储桶不存在" }
        require(!state.isReadOnly()) { "只读存储桶无法更新容量" }

        // 验证新容量不小于已使用容量
        require(command.maxCapacity >= state.usedCapacity) { 
            "新容量不能小于已使用容量: ${state.usedCapacity}" 
        }

        val newMaxFileSize = command.maxFileSize ?: state.maxFileSize

        // 验证单文件大小不超过总容量
        require(newMaxFileSize <= command.maxCapacity) { 
            "单文件最大大小不能超过存储桶总容量" 
        }

        return BucketCapacityUpdated(
            id = command.id,
            oldMaxCapacity = state.maxCapacity,
            newMaxCapacity = command.maxCapacity,
            oldMaxFileSize = state.maxFileSize,
            newMaxFileSize = newMaxFileSize
        )
    }

    @OnCommand
    fun onUpdatePermissions(command: UpdateBucketPermissions): BucketPermissionsUpdated {
        logger.info("更新存储桶权限: {}", command.id)

        // 验证存储桶存在且有管理权限
        require(state.exists()) { "存储桶不存在" }
        // TODO: 验证用户管理权限

        // 验证文件类型格式
        command.allowedTypes?.forEach { type ->
            require(type.matches(Regex("^[a-zA-Z0-9]+/[a-zA-Z0-9+.-]+$"))) { 
                "文件类型格式不正确: $type" 
            }
        }

        // 验证保留天数
        command.retentionDays?.let { days ->
            require(days > 0) { "文件保留天数必须大于0" }
        }

        return BucketPermissionsUpdated(
            id = command.id,
            isPublic = command.isPublic,
            allowedTypes = command.allowedTypes,
            enableCompression = command.enableCompression,
            enableEncryption = command.enableEncryption,
            retentionDays = command.retentionDays
        )
    }

    @OnCommand
    fun onUpdateStatus(command: UpdateBucketStatus): BucketStatusUpdated {
        logger.info("更新存储桶状态: {} -> {}", state.status, command.status)

        // 验证存储桶存在
        require(state.exists()) { "存储桶不存在" }

        // 验证状态转换是否合法
        require(state.canTransitionTo(command.status)) { 
            "无法从 ${state.status} 转换到 ${command.status}" 
        }

        // 特殊状态验证
        when (command.status) {
            BucketStatus.ACTIVE -> {
                require(!state.isFull()) { "存储桶已满，无法设置为活跃状态" }
            }
            else -> { /* 其他状态无特殊要求 */ }
        }

        return BucketStatusUpdated(
            id = command.id,
            oldStatus = state.status,
            newStatus = command.status
        )
    }

    @OnCommand
    fun onUpdateUsage(command: UpdateBucketUsage): BucketUsageUpdated {
        logger.info("更新存储桶使用量: {} bytes", command.sizeDelta)

        // 验证存储桶存在
        require(state.exists()) { "存储桶不存在" }

        val newUsedCapacity = state.usedCapacity + command.sizeDelta

        // 验证使用量不能为负数
        require(newUsedCapacity >= 0) { "使用容量不能为负数" }

        // 验证不超过最大容量(允许临时超出，但会发出警告)
        if (newUsedCapacity > state.maxCapacity) {
            logger.warn("存储桶使用量超出最大容量: {} > {}", newUsedCapacity, state.maxCapacity)
        }

        val usagePercentage = if (state.maxCapacity > 0) {
            (newUsedCapacity.toDouble() / state.maxCapacity.toDouble()) * 100.0
        } else {
            0.0
        }

        return BucketUsageUpdated(
            id = command.id,
            oldUsedCapacity = state.usedCapacity,
            newUsedCapacity = newUsedCapacity,
            sizeDelta = command.sizeDelta,
            operation = command.operation,
            usagePercentage = usagePercentage
        )
    }

    @OnCommand
    fun onValidate(command: ValidateBucket): Any {
        logger.info("验证存储桶: 所需容量={}", command.requiredCapacity)

        // 检查存储桶是否存在
        if (!state.exists()) {
            return BucketValidationFailed(
                id = command.id,
                reason = "存储桶不存在",
                requiredCapacity = command.requiredCapacity,
                availableCapacity = 0
            )
        }

        // 检查存储桶状态
        if (!state.isAvailableForUpload()) {
            return BucketValidationFailed(
                id = command.id,
                reason = "存储桶状态不允许上传: ${state.status}",
                requiredCapacity = command.requiredCapacity,
                availableCapacity = state.getAvailableCapacity()
            )
        }

        // 检查容量
        if (!state.hasCapacity(command.requiredCapacity)) {
            return BucketValidationFailed(
                id = command.id,
                reason = "存储桶容量不足",
                requiredCapacity = command.requiredCapacity,
                availableCapacity = state.getAvailableCapacity()
            )
        }

        // 检查文件类型
        command.fileType?.let { fileType ->
            if (!state.isFileTypeAllowed(fileType)) {
                return BucketValidationFailed(
                    id = command.id,
                    reason = "不支持的文件类型: $fileType",
                    requiredCapacity = command.requiredCapacity,
                    availableCapacity = state.getAvailableCapacity()
                )
            }
        }

        return BucketValidated(
            id = command.id,
            isValid = true,
            availableCapacity = state.getAvailableCapacity()
        )
    }

    @OnCommand
    fun onCleanupExpiredFiles(command: CleanupExpiredFiles): Mono<ExpiredFilesCleanedUp> {
        logger.info("清理过期文件: 存储桶={}, 干运行={}", command.id, command.dryRun)

        // 验证存储桶存在
        require(state.exists()) { "存储桶不存在" }

        return Mono.fromCallable {
            // 模拟清理过程
            val estimatedCleanupSpace = state.getEstimatedCleanupSpace()
            val estimatedFileCount = (estimatedCleanupSpace / (1024 * 1024)).toInt() // 假设平均1MB每文件

            ExpiredFilesCleanedUp(
                id = command.id,
                cleanedFileCount = estimatedFileCount,
                freedCapacity = estimatedCleanupSpace,
                dryRun = command.dryRun
            )
        }
    }

    @OnCommand
    fun onDelete(command: DeleteStorageBucket): StorageBucketDeleted {
        logger.info("删除存储桶: {}", command.id)

        // 验证可以删除
        require(state.canDelete()) { "存储桶无法删除：存在文件或状态不允许" }

        return StorageBucketDeleted(
            id = command.id,
            deletedTime = LocalDateTime.now()
        )
    }

    /**
     * 容量变化时的后续处理
     */
    fun handleCapacityChange(): List<Any> {
        val events = mutableListOf<Any>()

        // 检查是否需要容量警告
        if (state.needsCapacityWarning()) {
            val warningLevel = state.getCapacityWarningLevel()!!
            events.add(
                BucketCapacityWarning(
                    id = "current", // 在实际实现中应该是聚合根ID
                    usedCapacity = state.usedCapacity,
                    maxCapacity = state.maxCapacity,
                    usagePercentage = state.getUsagePercentage(),
                    warningLevel = warningLevel
                )
            )
        }

        // 自动状态调整
        val suggestedStatus = state.getSuggestedStatus()
        if (suggestedStatus != state.status && state.canTransitionTo(suggestedStatus)) {
            events.add(
                BucketStatusUpdated(
                    id = "current",
                    oldStatus = state.status,
                    newStatus = suggestedStatus,
                    reason = "自动状态调整"
                )
            )
        }

        return events
    }

    /**
     * 验证存储桶名称
     */
    private fun validateBucketName(bucketName: String) {
        require(bucketName.isNotBlank()) { "存储桶名称不能为空" }
        require(bucketName.length in 3..63) { "存储桶名称长度必须在3-63个字符之间" }
        require(bucketName.matches(Regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$"))) { 
            "存储桶名称只能包含小写字母、数字和连字符，且不能以连字符开头或结尾" 
        }
        require(!bucketName.contains("--")) { "存储桶名称不能包含连续的连字符" }
    }

    /**
     * 验证存储桶配置
     */
    private fun validateBucketConfiguration(
        maxCapacity: Long,
        maxFileSize: Long,
        retentionDays: Int,
        allowedTypes: Set<String>
    ) {
        require(maxCapacity > 0) { "最大容量必须大于0" }
        require(maxFileSize > 0) { "单文件最大大小必须大于0" }
        require(maxFileSize <= maxCapacity) { "单文件最大大小不能超过存储桶总容量" }
        require(retentionDays > 0) { "文件保留天数必须大于0" }

        // 验证文件类型格式
        allowedTypes.forEach { type ->
            require(type.matches(Regex("^[a-zA-Z0-9]+/[a-zA-Z0-9+.-]+$"))) { 
                "文件类型格式不正确: $type" 
            }
        }
    }

    /**
     * 计算推荐的存储桶配置
     */
    fun getRecommendedConfiguration(): Map<String, Any> {
        return mapOf(
            "recommendedCleanupInterval" to "7天",
            "recommendedCapacityWarningThreshold" to "80%",
            "recommendedMaxFileSize" to (state.maxCapacity * 0.1).toLong(),
            "estimatedMonthlyGrowth" to calculateEstimatedGrowth(),
            "compressionPotential" to calculateCompressionPotential()
        )
    }

    /**
     * 计算预估增长
     */
    private fun calculateEstimatedGrowth(): Long {
        // 简化计算，实际应该基于历史数据
        return (state.usedCapacity * 0.1).toLong() // 假设每月增长10%
    }

    /**
     * 计算压缩潜力
     */
    private fun calculateCompressionPotential(): Double {
        // 简化计算，实际应该基于文件类型分析
        return if (state.enableCompression) 0.3 else 0.0 // 假设可压缩30%
    }
}