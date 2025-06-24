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

    @field:Size(max = 64)
    val username: String? = null,

    val accountId: String? = null,

    @field:Size(max = 128)
    val primaryEmail: String? = null,

    val primaryPhone: String? = null,

    @field:Size(max = 2048)
    val avatar: String? = null,
)

data class UserCreated(
    val name: String,
    val accountId: String?,
    val primaryEmail: String?,
    val primaryPhone: String?,
    val avatar: String?,
    val username: String?,
)
