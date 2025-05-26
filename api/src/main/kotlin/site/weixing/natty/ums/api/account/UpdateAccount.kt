package site.weixing.natty.ums.api.account

import jakarta.validation.constraints.Email
import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新账户"
)
data class UpdateAccount(
    @CommandRoute.PathVariable val id: String,

    val username: String? = null,

    @field:Email
    val email: String? = null,

    val phone: String? = null
)

data class AccountUpdated(
    val accountId: String,
    val username: String?,
    val email: String?,
    val phone: String?
)
