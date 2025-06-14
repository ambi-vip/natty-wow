package site.weixing.natty.api.crm.receivable

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate
import java.math.BigDecimal
import java.time.LocalDate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建应收"
)
data class CreateReceivable(
    @field:NotBlank
    val contractId: String,

    @field:NotNull
    val amount: BigDecimal,

    @field:NotNull
    val dueDate: LocalDate,

    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新应收"
)
data class UpdateReceivable(
    @field:NotBlank
    val id: String,

    val amount: BigDecimal? = null,

    val dueDate: LocalDate? = null,

    val remark: String? = null
)

@CommandRoute(
    summary = "删除应收"
)
data class DeleteReceivable(@CommandRoute.PathVariable val id: String) : DeleteAggregate

@CommandRoute(
    method = CommandRoute.Method.PATCH,
    action = "/pay",
    summary = "标记应收已支付"
)
data class MarkReceivableAsPaid(
    @field:NotBlank
    val id: String
)

data class ReceivableCreated(
    val contractId: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
    val remark: String?
)

data class ReceivableUpdated(
    val amount: BigDecimal?,
    val dueDate: LocalDate?,
    val remark: String?
)

data class ReceivableDeleted(val id: String)

data class ReceivablePaid(val id: String)