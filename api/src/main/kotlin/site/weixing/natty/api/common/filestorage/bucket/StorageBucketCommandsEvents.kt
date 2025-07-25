package site.weixing.natty.api.storage.bucket

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate
import me.ahoo.wow.api.command.validation.CommandValidator
import site.weixing.natty.api.common.filestorage.bucket.BucketStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建存储桶",
    description = "创建新的存储桶用于文件存储"
)
data class CreateStorageBucket(
    @field:NotBlank(message = "存储桶名称不能为空")
    @field:Size(min = 3, max = 63, message = "存储桶名称长度必须在3-63个字符之间")
    val bucketName: String,

    val ownerId: String? = null,

    @field:NotNull(message = "最大容量不能为空")
    val maxCapacity: Long,

    val maxFileSize: Long = 100 * 1024 * 1024 * 1024L, // 默认100GB

    val allowedTypes: Set<String> = emptySet(),

    val isPublic: Boolean = false,

    val enableCompression: Boolean = true,

    val enableEncryption: Boolean = false,

    val retentionDays: Int = 30,

    @field:Size(max = 500, message = "描述长度不能超过500个字符")
    val description: String? = null
) : CommandValidator {
    override fun validate() {
        require(maxCapacity > 0) { "最大容量必须大于0" }
        require(maxFileSize > 0) { "单文件最大大小必须大于0" }
        require(retentionDays > 0) { "文件保留天数必须大于0" }
        
        // 验证存储桶名称格式
        require(bucketName.matches(Regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$"))) { 
            "存储桶名称只能包含小写字母、数字和连字符，且不能以连字符开头或结尾" 
        }
        
        // 验证文件类型格式
        allowedTypes.forEach { type ->
            require(type.matches(Regex("^[a-zA-Z0-9]+/[a-zA-Z0-9+.-]+$"))) { 
                "文件类型格式不正确: $type" 
            }
        }
    }
}

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "/capacity",
    summary = "更新存储桶容量",
    description = "更新存储桶的最大容量和单文件大小限制"
)
data class UpdateBucketCapacity(
    @CommandRoute.PathVariable
    val id: String,

    @field:NotNull(message = "最大容量不能为空")
    val maxCapacity: Long,

    val maxFileSize: Long? = null
) : CommandValidator {
    override fun validate() {
        require(maxCapacity > 0) { "最大容量必须大于0" }
        maxFileSize?.let { 
            require(it > 0) { "单文件最大大小必须大于0" }
        }
    }
}

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "/permissions",
    summary = "更新存储桶权限",
    description = "更新存储桶的访问权限和配置"
)
data class UpdateBucketPermissions(
    @CommandRoute.PathVariable
    val id: String,

    val isPublic: Boolean? = null,

    val allowedTypes: Set<String>? = null,

    val enableCompression: Boolean? = null,

    val enableEncryption: Boolean? = null,

    val retentionDays: Int? = null
) : CommandValidator {
    override fun validate() {
        retentionDays?.let { 
            require(it > 0) { "文件保留天数必须大于0" }
        }
        
        allowedTypes?.forEach { type ->
            require(type.matches(Regex("^[a-zA-Z0-9]+/[a-zA-Z0-9+.-]+$"))) { 
                "文件类型格式不正确: $type" 
            }
        }
    }
}

@CommandRoute(
    method = CommandRoute.Method.PATCH,
    action = "/status",
    summary = "更新存储桶状态",
    description = "更新存储桶的运行状态"
)
data class UpdateBucketStatus(
    @CommandRoute.PathVariable
    val id: String,

    @field:NotNull(message = "状态不能为空")
    val status: BucketStatus
)

@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/usage/update",
    summary = "更新使用量",
    description = "更新存储桶的已使用容量"
)
data class UpdateBucketUsage(
    @CommandRoute.PathVariable
    val id: String,

    val sizeDelta: Long,

    val operation: UsageOperation
) : CommandValidator {
    override fun validate() {
        require(sizeDelta != 0L) { "容量变化不能为0" }
        
        when (operation) {
            UsageOperation.ADD -> require(sizeDelta > 0) { "添加操作的容量变化必须为正数" }
            UsageOperation.REMOVE -> require(sizeDelta < 0) { "删除操作的容量变化必须为负数" }
        }
    }
}

@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/validate",
    summary = "验证存储桶",
    description = "验证存储桶状态和容量是否可用"
)
data class ValidateBucket(
    @CommandRoute.PathVariable
    val id: String,

    val requiredCapacity: Long,

    val fileType: String? = null
) : CommandValidator {
    override fun validate() {
        require(requiredCapacity > 0) { "所需容量必须大于0" }
    }
}

@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/cleanup",
    summary = "清理过期文件",
    description = "清理存储桶中的过期文件"
)
data class CleanupExpiredFiles(
    @CommandRoute.PathVariable
    val id: String,

    val dryRun: Boolean = true
)

@CommandRoute(
    summary = "删除存储桶"
)
data class DeleteStorageBucket(@CommandRoute.PathVariable val id: String) : DeleteAggregate


enum class UsageOperation {
    ADD,    // 增加使用量
    REMOVE  // 减少使用量
}

// 事件类
data class StorageBucketCreated(
    val bucketName: String,
    val ownerId: String?,
    val maxCapacity: Long,
    val maxFileSize: Long,
    val allowedTypes: Set<String>,
    val isPublic: Boolean,
    val enableCompression: Boolean,
    val enableEncryption: Boolean,
    val retentionDays: Int,
    val description: String?
)

data class BucketCapacityUpdated(
    val id: String,
    val oldMaxCapacity: Long,
    val newMaxCapacity: Long,
    val oldMaxFileSize: Long?,
    val newMaxFileSize: Long?
)

data class BucketPermissionsUpdated(
    val id: String,
    val isPublic: Boolean?,
    val allowedTypes: Set<String>?,
    val enableCompression: Boolean?,
    val enableEncryption: Boolean?,
    val retentionDays: Int?
)

data class BucketStatusUpdated(
    val id: String,
    val oldStatus: BucketStatus,
    val newStatus: BucketStatus,
    val reason: String? = null
)

data class BucketUsageUpdated(
    val id: String,
    val oldUsedCapacity: Long,
    val newUsedCapacity: Long,
    val sizeDelta: Long,
    val operation: UsageOperation,
    val usagePercentage: Double
)

data class BucketValidated(
    val id: String,
    val isValid: Boolean,
    val availableCapacity: Long,
    val reason: String? = null
)

data class BucketValidationFailed(
    val id: String,
    val reason: String,
    val requiredCapacity: Long,
    val availableCapacity: Long
)

data class ExpiredFilesCleanedUp(
    val id: String,
    val cleanedFileCount: Int,
    val freedCapacity: Long,
    val dryRun: Boolean
)

data class BucketCapacityWarning(
    val id: String,
    val usedCapacity: Long,
    val maxCapacity: Long,
    val usagePercentage: Double,
    val warningLevel: CapacityWarningLevel
)

data class StorageBucketDeleted(
    val id: String,
    val deletedTime: LocalDateTime = LocalDateTime.now()
)

enum class CapacityWarningLevel {
    LOW,      // 80%
    HIGH,     // 90%
    CRITICAL  // 95%
}