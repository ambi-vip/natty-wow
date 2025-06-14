package site.weixing.natty.api.crm.contact

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建联系人"
)
data class CreateContact(
    @field:NotBlank
    val name: String,
    
    @field:NotBlank
    val phone: String,
    
    val email: String? = null,
    
    val position: String? = null,
    
    val customerId: String,
    
    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新联系人"
)
data class UpdateContact(
    @field:NotBlank
    val id: String,
    
    val name: String? = null,
    
    val phone: String? = null,
    
    val email: String? = null,
    
    val position: String? = null,
    
    val remark: String? = null
)

@CommandRoute(
    summary = "删除联系人"
)
data class DeleteContact(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class ContactCreated(
    val name: String,
    val phone: String,
    val email: String?,
    val position: String?,
    val customerId: String,
    val remark: String?
)

data class ContactUpdated(
    val name: String?,
    val phone: String?,
    val email: String?,
    val position: String?,
    val remark: String?
)

data class ContactDeleted(
    val id: String
)