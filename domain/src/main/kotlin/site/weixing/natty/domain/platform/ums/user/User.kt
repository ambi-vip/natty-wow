package site.weixing.natty.domain.platform.ums.user

import io.swagger.v3.oas.annotations.tags.Tag
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import site.weixing.natty.platform.api.ums.ChangePassword
import site.weixing.natty.platform.api.ums.CreateUser
import site.weixing.natty.platform.api.ums.PasswordChanged
import site.weixing.natty.platform.api.ums.ResetPwd
import site.weixing.natty.platform.api.ums.UpdateUser
import site.weixing.natty.platform.api.ums.UserCreated
import site.weixing.natty.platform.api.ums.UserUpdated

@Suppress("unused")
@AggregateRoot
@StaticTenantId
@Tag(name = "User")
class User(private val state: UserState) {

    @OnCommand
    fun onCreate(command: CreateUser): UserCreated {
        return UserCreated(
            username = command.username,
            email = command.email,
            phone = command.phone,
            nickname = command.nickname,
            status = command.status,
            roleIds = command.roleIds
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateUser): UserUpdated {
        return UserUpdated(
            email = command.email,
            phone = command.phone,
            nickname = command.nickname,
            status = command.status,
            roleIds = command.roleIds
        )
    }

    @OnCommand
    fun onChangePassword(command: ChangePassword): PasswordChanged {
        // @See: 验证旧密码
        return PasswordChanged(
            userId = command.id
        )
    }

    @OnCommand
    fun onResetPassword(command: ResetPwd): PasswordChanged {
        return PasswordChanged(
            userId = state.id
        )
    }
}
