package site.weixing.natty.ums.api.account

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "/roles",
    summary = "分配角色"
)
data class AssignRole(
    @CommandRoute.PathVariable val id: String,
    val roleIds: Set<String>
)

data class RoleAssigned(
    val roleIds: Set<String>
)
