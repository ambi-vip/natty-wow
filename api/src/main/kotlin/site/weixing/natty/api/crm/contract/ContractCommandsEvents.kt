package site.weixing.natty.api.crm.contract

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
    summary = "创建合同"
)
data class CreateContract(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val customerId: String,

    @field:NotBlank
    val businessId: String,

    @field:NotNull
    val amount: BigDecimal,

    @field:NotNull
    val signDate: LocalDate,

    val startDate: LocalDate? = null,

    val endDate: LocalDate? = null,

    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新合同"
)
data class UpdateContract(
    @field:NotBlank
    val id: String,

    val name: String? = null,

    val amount: BigDecimal? = null,

    val signDate: LocalDate? = null,

    val startDate: LocalDate? = null,

    val endDate: LocalDate? = null,

    val remark: String? = null
)

@CommandRoute(
    summary = "删除合同"
)
data class DeleteContract(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class ContractCreated(
    val name: String,
    val customerId: String,
    val businessId: String,
    val amount: BigDecimal,
    val signDate: LocalDate,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val remark: String?
)

data class ContractUpdated(
    val name: String?,
    val amount: BigDecimal?,
    val signDate: LocalDate?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val remark: String?
)

data class ContractDeleted(val id: String) 
