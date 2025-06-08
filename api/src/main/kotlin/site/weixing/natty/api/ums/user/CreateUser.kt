package site.weixing.natty.ums.api.user

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建用户"
)
data class CreateUser(
    @field:NotBlank
    val name: String,

    val accountId: String? = null,

    val email: String? = null,

    val phone: String? = null,

    val avatar: String? = null
)

data class UserCreated(
    val name: String,
    val accountId: String?,
    val email: String?,
    val phone: String?,
    val avatar: String?
)
