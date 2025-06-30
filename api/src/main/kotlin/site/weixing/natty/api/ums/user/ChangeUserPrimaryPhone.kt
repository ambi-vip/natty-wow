package site.weixing.natty.api.ums.user

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "primary-phone",
    summary = "修改用户手机号"
)
data class ChangeUserPrimaryPhone(
    @field:NotBlank
    val newPhone: String
)

data class UserPrimaryPhoneChanged(
    val oldPhone: String?,
    val newPhone: String,
    val changedAt: Long = System.currentTimeMillis()
)
