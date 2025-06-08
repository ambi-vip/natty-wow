package site.weixing.natty.api.ums.account

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "delete",
    summary = "删除账户"
)
data class DeleteAccount(
    @CommandRoute.PathVariable val id: String
)

data class AccountDeleted(
    val accountId: String
)
