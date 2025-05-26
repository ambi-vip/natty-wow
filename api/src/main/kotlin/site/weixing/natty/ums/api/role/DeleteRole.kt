package site.weixing.natty.ums.api.role

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.command.DeleteAggregate

@CommandRoute(
    summary = "删除角色"
)
data class DeleteRole(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class RoleDeleted(
    val roleId: String
)
