package site.weixing.natty.api.crm.clue

import jakarta.validation.Valid
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
    summary = "创建线索"
)
data class CreateClue(
    @field:NotBlank(message = "线索名称不能为空")
    val name: String,

    val ownerUserId: String? = null, // TODO: 考虑使用实际的用户ID类型
    @field:Valid
    val contactInfo: ContactInfo,

    val areaId: String? = null, // TODO: 考虑使用地区枚举或特定类型
    val detailAddress: String? = null,
    val industryId: String? = null, // TODO: 考虑使用行业枚举或特定类型
    val level: String? = null, // TODO: 考虑使用客户等级枚举或特定类型
    val source: String? = null, // TODO: 考虑使用线索来源枚举或特定类型
    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新线索"
)
data class UpdateClue(
    @CommandRoute.PathVariable
    val id: String,

    @field:NotNull(message = "线索不存在")
    @field:NotBlank(message = "线索名称不能为空")
    val name: String? = null,
    val ownerUserId: String? = null,
    @field:NotNull(message = "联系方式不能为空")
    val contactInfo: ContactInfo? = null,

    val areaId: String? = null,
    val detailAddress: String? = null,
    val industryId: String? = null,
    val level: String? = null,
    val source: String? = null,
    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PATCH,
    action = "/transform",
    summary = "线索转化为客户"
)
data class TransformClue(
    @field:NotBlank
    val id: String,
    @field:NotBlank(message = "客户ID不能为空")
    val customerId: String,
    val transformType: String? = null, // TODO: 考虑使用枚举或特定类型
    val transformReason: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PATCH,
    action = "/transfer",
    summary = "线索转移负责人"
)
data class TransferClue(
    @field:NotBlank
    val id: String,
    @field:NotBlank(message = "新负责人ID不能为空")
    val newOwnerUserId: String
)

@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/followup",
    summary = "添加线索跟进记录"
)
data class AddClueFollowUp(
    @field:NotBlank
    val clueId: String,
    @field:NotBlank
    val content: String,
    @field:NotNull
    val nextTime: LocalDateTime
)

@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/transform-record",
    summary = "添加线索转化记录"
)
data class AddClueTransformRecord(
    @field:NotBlank
    val clueId: String,
    @field:NotBlank
    val customerId: String,
    val transformType: String? = null, // TODO: 考虑使用枚举或特定类型
    val transformReason: String? = null
)

@CommandRoute(
    summary = "删除线索"
)
data class DeleteClue(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class ClueCreated(
    val name: String,
    val ownerUserId: String?,
    val contactInfo: ContactInfo,
    val areaId: String?,
    val detailAddress: String?,
    val industryId: String?,
    val level: String?,
    val source: String?,
    val remark: String?
)

data class ClueUpdated(
    val name: String?,
    val ownerUserId: String?,
    val contactInfo: ContactInfo?,
    val areaId: String?,
    val detailAddress: String?,
    val industryId: String?,
    val level: String?,
    val source: String?,
    val remark: String?
)

data class ClueTransformed(
    val id: String,
    val customerId: String,
    val transformType: String?,
    val transformReason: String?
)

data class ClueTransferred(
    val id: String,
    val oldOwnerUserId: String?,
    val newOwnerUserId: String
)

data class ClueFollowUpAdded(
    val clueId: String,
    val followUpId: String, // 由聚合根生成
    val content: String,
    val nextTime: LocalDateTime
)

data class ClueTransformRecordAdded(
    val clueId: String,
    val recordId: String, // 由聚合根生成
    val customerId: String,
    val transformType: String?,
    val transformReason: String?
)

data class ClueDeleted(
    val id: String
) 