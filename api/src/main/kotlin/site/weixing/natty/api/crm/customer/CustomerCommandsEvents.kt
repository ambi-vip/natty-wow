package site.weixing.natty.api.crm.customer

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建客户"
)
data class CreateCustomer(
    @field:NotBlank
    val name: String,
    
    val phone: String? = null,
    
    val email: String? = null,
    
    val address: String? = null,
    
    val remark: String? = null,

    val source: String? = null,
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新客户"
)
data class UpdateCustomer(
    @field:NotBlank
    val id: String,
    
    val name: String? = null,
    
    val phone: String? = null,
    
    val email: String? = null,
    
    val address: String? = null,
    
    val remark: String? = null,

)

@CommandRoute(
    summary = "删除客户"
)
data class DeleteCustomer(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class CustomerCreated(
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val remark: String?,
    val source: String?,
)

data class CustomerUpdated(
    val name: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val remark: String?
)

data class CustomerDeleted(val id: String) 