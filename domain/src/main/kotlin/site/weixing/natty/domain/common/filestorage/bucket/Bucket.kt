package site.weixing.natty.domain.common.filestorage.bucket

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.bucket.*

/**
 * 存储桶聚合根
 * 管理文件存储桶的创建、配额管理、状态变更等操作
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class Bucket(
    private val state: BucketState
) {

    companion object {
        private val logger = logger {}
    }

    /**
     * 创建存储桶
     */
    @OnCommand
    fun onCreate(command: CreateBucket): Mono<BucketCreated> {
        logger.info { "创建存储桶: bucketName=${command.bucketName}, storageId=${command.storageId}" }

        // 业务规则验证
        validateBucketName(command.bucketName)
        validateQuotaLimit(command.quotaLimit)
        ensureNotExists()

        return BucketCreated(
            bucketName = command.bucketName,
            storageId = command.storageId,
            quotaLimit = command.quotaLimit,
            description = command.description,
            tags = command.tags
        ).toMono()
            .doOnSuccess {
                logger.info { "存储桶创建成功: bucketName=${command.bucketName}" }
            }
            .doOnError { error ->
                logger.error(error) { "存储桶创建失败: bucketName=${command.bucketName}" }
            }
    }

    /**
     * 更新存储桶配置
     */
    @OnCommand
    fun onUpdate(command: UpdateBucket): Mono<BucketUpdated> {
        logger.info { "更新存储桶配置: bucketName=${state.bucketName}" }

        ensureNotDeleted()

        // 验证配额变更
        command.quotaLimit?.let { newQuotaLimit ->
            validateQuotaLimit(newQuotaLimit)
            // 检查新配额是否小于当前使用量
            if (newQuotaLimit < state.usedSize) {
                throw IllegalArgumentException("新配额 $newQuotaLimit 不能小于当前使用量 ${state.usedSize}")
            }
        }

        return BucketUpdated(
            quotaLimit = command.quotaLimit,
            description = command.description,
            tags = command.tags
        ).toMono()
            .doOnSuccess {
                logger.info { "存储桶配置更新成功: bucketName=${state.bucketName}" }
            }
    }

    /**
     * 删除存储桶
     */
    @OnCommand
    fun onDelete(command: DeleteBucket): Mono<BucketDeleted> {
        logger.info { "删除存储桶: bucketName=${state.bucketName}" }

        ensureNotDeleted()

        // 检查是否有文件存在
        if (state.fileCount > 0 && !command.force) {
            throw IllegalStateException("存储桶中还有 ${state.fileCount} 个文件，请先清空或使用强制删除")
        }

        return BucketDeleted(
            bucketName = state.bucketName ?: "unknown"
        ).toMono()
            .doOnSuccess {
                logger.info { "存储桶删除成功: bucketName=${state.bucketName}" }
            }
    }

    /**
     * 改变存储桶状态
     */
    @OnCommand
    fun onChangeStatus(command: ChangeBucketStatus): Mono<BucketStatusChanged> {
        logger.info { "改变存储桶状态: bucketName=${state.bucketName}, newStatus=${command.newStatus}" }

        ensureNotDeleted()

        val oldStatus = state.status
        if (oldStatus == command.newStatus) {
            throw IllegalArgumentException("存储桶状态已经是 ${command.newStatus}")
        }

        // 状态变更业务规则
        validateStatusTransition(oldStatus, command.newStatus)

        return BucketStatusChanged(
            oldStatus = oldStatus,
            newStatus = command.newStatus,
            reason = command.reason
        ).toMono()
            .doOnSuccess {
                logger.info { "存储桶状态变更成功: ${oldStatus} -> ${command.newStatus}" }
            }
    }

    /**
     * 增加文件使用量
     */
    @OnCommand
    fun onAddFileUsage(command: AddFileUsage): Mono<FileUsageAdded> {
        logger.debug { "增加文件使用量: bucketName=${state.bucketName}, fileSize=${command.fileSize}" }

        ensureNotDeleted()
        ensureActive()

        val newUsedSize = state.usedSize + command.fileSize
        val newFileCount = state.fileCount + 1

        // 检查配额限制
        if (state.quotaLimit > 0 && newUsedSize > state.quotaLimit) {
            val remainingQuota = state.quotaLimit - state.usedSize
            throw IllegalStateException("存储配额不足：需要 ${command.fileSize} 字节，剩余 $remainingQuota 字节")
        }

        return FileUsageAdded(
            fileId = command.fileId,
            fileSize = command.fileSize,
            newUsedSize = newUsedSize,
            newFileCount = newFileCount
        ).toMono()
            .doOnSuccess {
                logger.debug { "文件使用量增加成功: fileId=${command.fileId}, fileSize=${command.fileSize}" }
            }
    }

    /**
     * 减少文件使用量
     */
    @OnCommand
    fun onRemoveFileUsage(command: RemoveFileUsage): Mono<FileUsageRemoved> {
        logger.debug { "减少文件使用量: bucketName=${state.bucketName}, fileSize=${command.fileSize}" }

        ensureNotDeleted()

        val newUsedSize = maxOf(0, state.usedSize - command.fileSize)
        val newFileCount = maxOf(0, state.fileCount - 1)

        return FileUsageRemoved(
            fileId = command.fileId,
            fileSize = command.fileSize,
            newUsedSize = newUsedSize,
            newFileCount = newFileCount
        ).toMono()
            .doOnSuccess {
                logger.debug { "文件使用量减少成功: fileId=${command.fileId}, fileSize=${command.fileSize}" }
            }
    }

    /**
     * 验证存储桶名称
     */
    private fun validateBucketName(bucketName: String) {
        require(bucketName.isNotBlank()) { "存储桶名称不能为空" }
        require(bucketName.length in 3..63) { "存储桶名称长度必须在3-63个字符之间" }
        require(bucketName.matches(Regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$"))) {
            "存储桶名称只能包含小写字母、数字和短横线，且必须以字母或数字开头和结尾"
        }
        require(!bucketName.contains("--")) { "存储桶名称不能包含连续的短横线" }
    }

    /**
     * 验证配额限制
     */
    private fun validateQuotaLimit(quotaLimit: Long) {
        require(quotaLimit >= 0) { "配额限制不能为负数" }
        // 0 表示无限制
        if (quotaLimit > 0) {
            val minQuota = 1024L * 1024L // 1MB 最小配额
            val maxQuota = 500L * 1024L * 1024L // 500MB 最大配额
            require(quotaLimit >= minQuota) { "配额限制不能小于 ${formatFileSize(minQuota)}" }
            require(quotaLimit <= maxQuota) { "配额限制不能大于 ${formatFileSize(maxQuota)}" }
        }
    }

    /**
     * 验证状态转换
     */
    private fun validateStatusTransition(from: BucketStatus, to: BucketStatus) {
        val validTransitions = mapOf(
            BucketStatus.ACTIVE to setOf(BucketStatus.SUSPENDED, BucketStatus.ARCHIVED),
            BucketStatus.SUSPENDED to setOf(BucketStatus.ACTIVE, BucketStatus.ARCHIVED),
            BucketStatus.ARCHIVED to setOf(BucketStatus.ACTIVE)
        )

        val allowedStatuses = validTransitions[from]
        if (allowedStatuses == null || to !in allowedStatuses) {
            throw IllegalArgumentException("不允许的状态转换: $from -> $to")
        }
    }

    /**
     * 确保存储桶不存在
     */
    private fun ensureNotExists() {
        if (state.bucketName != null) {
            throw IllegalStateException("存储桶已存在: ${state.bucketName}")
        }
    }

    /**
     * 确保存储桶未被删除
     */
    private fun ensureNotDeleted() {
        if (state.status == BucketStatus.DELETED) {
            throw IllegalStateException("不能对已删除的存储桶进行操作")
        }
    }

    /**
     * 确保存储桶处于活动状态
     */
    private fun ensureActive() {
        if (state.status != BucketStatus.ACTIVE) {
            throw IllegalStateException("存储桶状态不是活动状态: ${state.status}")
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

    /**
     * 获取剩余配额
     */
    fun getRemainingQuota(): Long {
        return if (state.quotaLimit <= 0) {
            Long.MAX_VALUE // 无限制
        } else {
            maxOf(0, state.quotaLimit - state.usedSize)
        }
    }

    /**
     * 计算配额使用率
     */
    fun getQuotaUsagePercentage(): Double {
        return if (state.quotaLimit <= 0) {
            0.0 // 无限制时返回0%
        } else {
            (state.usedSize.toDouble() / state.quotaLimit.toDouble()) * 100.0
        }
    }

    /**
     * 检查是否接近配额限制
     */
    fun isNearQuotaLimit(threshold: Double = 90.0): Boolean {
        return state.quotaLimit > 0 && getQuotaUsagePercentage() >= threshold
    }
}