package site.weixing.natty.platform.api.ums

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建角色"
)
data class CreateRole(
    @field:NotBlank
    val name: String,
    
    val code: String? = null,
    
    val description: String? = null,
    
    val permissionIds: List<String> = emptyList()
)

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新角色"
)
data class UpdateRole(
    @CommandRoute.PathVariable 
    val id: String,
    
    val name: String? = null,
    
    val code: String? = null,
    
    val description: String? = null,
    
    val permissionIds: List<String>? = null
)

data class RoleCreated(
    val name: String,
    val code: String?,
    val description: String?,
    val permissionIds: List<String>
)

data class RoleUpdated(
    val name: String?,
    val code: String?,
    val description: String?,
    val permissionIds: List<String>?
) 