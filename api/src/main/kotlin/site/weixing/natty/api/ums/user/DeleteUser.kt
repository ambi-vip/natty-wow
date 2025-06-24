package site.weixing.natty.api.ums.user

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.Summary
import me.ahoo.wow.api.command.DeleteAggregate
import me.ahoo.wow.api.event.AggregateDeleted

@CommandRoute(
    method = CommandRoute.Method.DELETE,
    action = "",
    appendIdPath = CommandRoute.AppendPath.ALWAYS
)
@Summary("删除用户")
data class DeleteUser(
    val reason: String? = null
) : DeleteAggregate

data class UserDeleted(
    val reason: String?
) : AggregateDeleted