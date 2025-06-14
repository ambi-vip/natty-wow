package site.weixing.natty.api.crm.operatelog

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import java.time.LocalDateTime

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建操作日志"
)
data class CreateOperateLog(
    @field:NotBlank
    val operatorId: String,

    @field:NotBlank
    val operatorName: String,

    @field:NotBlank
    val operation: String,

    @field:NotBlank
    val targetType: String,

    @field:NotBlank
    val targetId: String,

    val remark: String? = null
)

data class OperateLogCreated(
    val operatorId: String,
    val operatorName: String,
    val operation: String,
    val targetType: String,
    val targetId: String,
    val remark: String?
) 