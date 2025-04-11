package site.weixing.natty.platform.api.ums

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.annotation.Order

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建用户",
    appendTenantPath = CommandRoute.AppendPath.NEVER
)
data class CreateUser(
    @field:NotBlank
    val username: String,

    @field:NotBlank
    val password: String,

    val email: String? = null,

    val phone: String? = null,

    val nickname: String? = null,

    val status: UserStatus = UserStatus.ENABLED,

    val roleIds: List<String> = emptyList()
)

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    summary = "更新用户"
)
data class UpdateUser(
    @CommandRoute.PathVariable
    val id: String,

    val email: String? = null,

    val phone: String? = null,

    val nickname: String? = null,

    val status: UserStatus? = null,

    val roleIds: List<String>? = null
)

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "password",
    summary = "修改密码"
)
data class ChangePassword(
    @CommandRoute.PathVariable
    val id: String,

    @field:NotBlank
    val oldPassword: String,

    @field:NotBlank
    val newPassword: String
)

@Order(4)
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.NEVER,
    action = "reset-pwd",
    summary = "重置密码"
)
data class ResetPwd(
    val phone: String,

    @field:NotBlank
    val oldPassword: String,

    @field:NotBlank
    val newPassword: String
)

enum class UserStatus {
    ENABLED,
    DISABLED,
    LOCKED
}

data class UserCreated(
    val username: String,
    val email: String?,
    val phone: String?,
    val nickname: String?,
    val status: UserStatus,
    val roleIds: List<String>
)

data class UserUpdated(
    val email: String?,
    val phone: String?,
    val nickname: String?,
    val status: UserStatus?,
    val roleIds: List<String>?
)

data class PasswordChanged(
    val userId: String
)
