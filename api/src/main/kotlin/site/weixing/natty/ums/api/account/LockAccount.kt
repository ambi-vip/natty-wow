package site.weixing.natty.ums.api.account

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "/lock",
    summary = "锁定账户"
)
data class LockAccount(
    @CommandRoute.PathVariable val id: String,
    val reason: String? = null
)

data class AccountLocked(
    val accountId: String,
    val reason: String?
)
