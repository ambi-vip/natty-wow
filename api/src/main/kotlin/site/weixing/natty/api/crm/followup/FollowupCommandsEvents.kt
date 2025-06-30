package site.weixing.natty.api.crm.followup

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate
import java.time.LocalDateTime

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建跟进记录"
)
data class CreateFollowup(
    @field:NotBlank(message = "跟进目标ID不能为空")
    val targetId: String,

    @field:NotBlank(message = "跟进目标类型不能为空")
    val targetType: String, // E.g., "CUSTOMER", "BUSINESS", "CLUE"

    @field:NotBlank
    val content: String,

    @field:NotNull
    @field:Future
    val followupTime: LocalDateTime,

    @field:NotBlank
    val followupMethod: String,

    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新跟进记录"
)
data class UpdateFollowup(
    @field:NotBlank
    val id: String,

    val content: String? = null,

    val followupTime: LocalDateTime? = null,

    val followupMethod: String? = null,

    val remark: String? = null
)

@CommandRoute(
    summary = "删除跟进记录"
)
data class DeleteFollowup(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class FollowupCreated(
    val targetId: String,
    val targetType: String,
    val content: String,
    val followupTime: LocalDateTime,
    val followupMethod: String,
    val remark: String?
)

data class FollowupUpdated(
    val content: String?,
    val followupTime: LocalDateTime?,
    val followupMethod: String?,
    val remark: String?
)

data class FollowupDeleted(val id: String) 
