package site.weixing.natty.api.ums.account

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "/unlock",
    summary = "解锁账户"
)
data class UnlockAccount(
    @CommandRoute.PathVariable val id: String
)

data class AccountUnlocked(
    val accountId: String
)
