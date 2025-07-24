package site.weixing.natty.api.common.filestorage.bucket

import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.command.validation.CommandValidator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min

/**
 * 创建存储桶命令
 */
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/create",
    summary = "创建存储桶"
)
data class CreateBucket(
    @field:NotBlank(message = "存储桶名称不能为空")
    val bucketName: String,

    @field:NotBlank(message = "存储配置ID不能为空")
    val storageId: String,

    @field:Min(value = 0, message = "配额限制不能为负数")
    val quotaLimit: Long = 0, // 0表示无限制

    val description: String? = null,

    val tags: List<String> = emptyList()
) : CommandValidator {

    override fun validate() {
        validateBucketName(bucketName)
        validateQuotaLimit(quotaLimit)
        validateTags(tags)
    }

    private fun validateBucketName(bucketName: String) {
        require(bucketName.length in 3..63) { "存储桶名称长度必须在3-63个字符之间" }
        require(bucketName.matches(Regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$"))) {
            "存储桶名称只能包含小写字母、数字和短横线，且必须以字母或数字开头和结尾"
        }
        require(!bucketName.contains("--")) { "存储桶名称不能包含连续的短横线" }
    }

    private fun validateQuotaLimit(quotaLimit: Long) {
        require(quotaLimit >= 0) { "配额限制不能为负数" }
        if (quotaLimit > 0) {
            val minQuota = 1024L * 1024L // 1MB
            val maxQuota = 500L * 1024L * 1024L // 500MB
            require(quotaLimit >= minQuota) { "配额限制不能小于1MB" }
            require(quotaLimit <= maxQuota) { "配额限制不能大于500MB" }
        }
    }

    private fun validateTags(tags: List<String>) {
        require(tags.size <= 10) { "标签数量不能超过10个" }
        tags.forEach { tag ->
            require(tag.isNotBlank()) { "标签不能为空" }
            require(tag.length <= 50) { "标签长度不能超过50个字符" }
        }
    }
}

/**
 * 更新存储桶命令
 */
@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "/update",
    summary = "更新存储桶配置"
)
data class UpdateBucket(
    val quotaLimit: Long? = null,
    val description: String? = null,
    val tags: List<String>? = null
) : CommandValidator {

    override fun validate() {
        quotaLimit?.let { validateQuotaLimit(it) }
        tags?.let { validateTags(it) }
    }

    private fun validateQuotaLimit(quotaLimit: Long) {
        require(quotaLimit >= 0) { "配额限制不能为负数" }
        if (quotaLimit > 0) {
            val minQuota = 1024L * 1024L // 1MB
            val maxQuota = 500L * 1024L * 1024L // 500MB
            require(quotaLimit >= minQuota) { "配额限制不能小于1MB" }
            require(quotaLimit <= maxQuota) { "配额限制不能大于500MB" }
        }
    }

    private fun validateTags(tags: List<String>) {
        require(tags.size <= 10) { "标签数量不能超过10个" }
        tags.forEach { tag ->
            require(tag.isNotBlank()) { "标签不能为空" }
            require(tag.length <= 50) { "标签长度不能超过50个字符" }
        }
    }
}

/**
 * 删除存储桶命令
 */
@CommandRoute(
    method = CommandRoute.Method.DELETE,
    action = "/delete",
    summary = "删除存储桶"
)
data class DeleteBucket(
    val force: Boolean = false, // 是否强制删除（即使有文件）
    val reason: String? = null
)

/**
 * 改变存储桶状态命令
 */
@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "/status",
    summary = "改变存储桶状态"
)
data class ChangeBucketStatus(
    @field:NotBlank(message = "新状态不能为空")
    val newStatus: BucketStatus,
    val reason: String? = null
) : CommandValidator {

    override fun validate() {
        // 不允许直接设置为删除状态，应该使用DeleteBucket命令
        require(newStatus != BucketStatus.DELETED) {
            "不能通过状态变更命令设置为删除状态，请使用删除命令"
        }
    }
}

/**
 * 增加文件使用量命令（内部命令，通常由文件上传触发）
 */
data class AddFileUsage(
    @field:NotBlank(message = "文件ID不能为空")
    val fileId: String,

    @field:Min(value = 1, message = "文件大小必须大于0")
    val fileSize: Long
)

/**
 * 减少文件使用量命令（内部命令，通常由文件删除触发）
 */
data class RemoveFileUsage(
    @field:NotBlank(message = "文件ID不能为空")
    val fileId: String,

    @field:Min(value = 1, message = "文件大小必须大于0")
    val fileSize: Long
)