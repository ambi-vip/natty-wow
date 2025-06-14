package site.weixing.natty.api.crm.business

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
    summary = "创建商机"
)
data class CreateBusiness(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val customerId: String,

    @field:NotNull
    val expectedRevenue: BigDecimal,

    val closeDate: LocalDate? = null,

    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新商机"
)
data class UpdateBusiness(
    @field:NotBlank
    val id: String,

    val name: String? = null,

    val expectedRevenue: BigDecimal? = null,

    val closeDate: LocalDate? = null,

    val remark: String? = null
)

@CommandRoute(
    summary = "删除商机"
)
data class DeleteBusiness(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class BusinessCreated(
    val name: String,
    val customerId: String,
    val expectedRevenue: BigDecimal,
    val closeDate: LocalDate?,
    val remark: String?
)

data class BusinessUpdated(
    val name: String?,
    val customerId: String? = null, // customerId is not expected to change after creation
    val expectedRevenue: BigDecimal?,
    val closeDate: LocalDate?,
    val remark: String?
)

data class BusinessDeleted(val id: String) 