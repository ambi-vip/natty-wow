package site.weixing.natty.api.crm.product

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate
import java.math.BigDecimal

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建产品"
)
data class CreateProduct(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val category: String,

    @field:NotNull
    val price: BigDecimal,

    val description: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新产品"
)
data class UpdateProduct(
    @field:NotBlank
    val id: String,

    val name: String? = null,

    val category: String? = null,

    val price: BigDecimal? = null,

    val description: String? = null
)

@CommandRoute(
    summary = "删除产品"
)
data class DeleteProduct(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class ProductCreated(
    val name: String,
    val category: String,
    val price: BigDecimal,
    val description: String?
)

data class ProductUpdated(
    val name: String?,
    val category: String?,
    val price: BigDecimal?,
    val description: String?
)

data class ProductDeleted(val id: String) 