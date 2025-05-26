package site.weixing.natty.ums.api.role

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

    val description: String? = null,

    val permissions: Set<String> = emptySet()
)

data class RoleCreated(
    val name: String,
    val description: String?,
    val permissions: Set<String>
)
