package site.weixing.natty.api.ums.user

import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import site.weixing.natty.api.ums.user.UserStatus

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "status",
    summary = "更新用户状态"
)
data class UpdateUserStatus(
    @field:NotNull
    val status: UserStatus,
    val reason: String? = null
)

data class UserStatusUpdated(
    val status: UserStatus,
    val reason: String?,
    val updatedAt: Long = System.currentTimeMillis()
) 