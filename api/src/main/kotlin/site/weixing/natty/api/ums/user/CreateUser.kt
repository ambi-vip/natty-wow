package site.weixing.natty.ums.api.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
    @field:Size(max = 64)
    val name: String,

    val accountId: String? = null,

    @field:Size(max = 128)
    val email: String? = null,

    val phone: String? = null,

    @field:Size(max = 2048)
    val avatar: String? = null,
)

data class UserCreated(
    val name: String,
    val accountId: String?,
    val email: String?,
    val phone: String?,
    val avatar: String?,
)
