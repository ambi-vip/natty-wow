package site.weixing.natty.api.ums.user

import jakarta.validation.constraints.Size
import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新用户基本信息"
)
data class UpdateUser(
    @field:Size(max = 64)
    val name: String? = null,

    @field:Size(max = 128)
    val email: String? = null,

    val phone: String? = null,

    @field:Size(max = 2048)
    val avatar: String? = null,
)

data class UserUpdated(
    val name: String?,
    val email: String?,
    val phone: String?,
    val avatar: String?,
) 