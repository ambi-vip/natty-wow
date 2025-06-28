package site.weixing.natty.api.ums.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.ahoo.wow.api.annotation.CommandRoute


@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "primary-email",
    summary = "修改用户邮箱"
)
data class ChangeUserPrimaryEmail(
    @field:NotBlank
    val oldEmail: String,

    @field:NotBlank
    @field:Size(max = 128)
    val newEmail: String
)

data class UserPrimaryEmailChanged(
    val oldEmail: String?,
    val newEmail: String,
    val changedAt: Long = System.currentTimeMillis()
)