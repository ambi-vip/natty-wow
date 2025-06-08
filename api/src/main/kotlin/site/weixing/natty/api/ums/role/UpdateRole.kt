package site.weixing.natty.ums.api.role

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.Event

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新角色"
)
data class UpdateRole(
    @CommandRoute.PathVariable val id: String,
    @field:NotBlank
    val name: String,
    val description: String?,
    val permissions: Set<String>
)

@Event
data class RoleUpdated(
    val roleId: String,
    val name: String,
    val description: String?,
    val permissions: Set<String>
)
