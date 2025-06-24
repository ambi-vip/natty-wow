package site.weixing.natty.ums.api.user

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "password",
    summary = "修改用户密码"
)
data class ChangeUserPassword(
    @field:NotBlank
    val oldPassword: String,
    
    @field:NotBlank
    val newPassword: String
)

data class UserPasswordChanged(
    val encryptedPassword: String,
    val encryptionMethod: String,
    val changedAt: Long = System.currentTimeMillis()
) 