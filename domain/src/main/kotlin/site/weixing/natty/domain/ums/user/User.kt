package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import me.ahoo.wow.api.command.CommandResultAccessor
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.ums.user.DeleteUser
import site.weixing.natty.api.ums.user.UpdateUser
import site.weixing.natty.api.ums.user.UpdateUserCustomData
//import site.weixing.natty.api.ums.user.UpdateUserIdentities
import site.weixing.natty.api.ums.user.UpdateUserProfile
import site.weixing.natty.api.ums.user.UpdateUserStatus
import site.weixing.natty.api.ums.user.UserCustomDataUpdated
import site.weixing.natty.api.ums.user.UserDeleted
//import site.weixing.natty.api.ums.user.UserIdentitiesUpdated
import site.weixing.natty.api.ums.user.UserProfileUpdated
import site.weixing.natty.api.ums.user.UserStatus
import site.weixing.natty.api.ums.user.UserStatusUpdated
import site.weixing.natty.api.ums.user.UserUpdated
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder
import site.weixing.natty.ums.api.user.ChangeUserPassword
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated
import site.weixing.natty.ums.api.user.UserPasswordChanged

@Suppress("unused")
@AggregateRoot
class User(private val state: UserState) {

    @OnCommand
    fun onCreate(
        command: CreateUser,
        saveUserPrepare: SaveUserPrepare,
        commandResultAccessor: CommandResultAccessor
    ): Mono<UserCreated> {

        return saveUserPrepare.bindPrepare(command, state)
            .then(
                Mono.fromCallable {
                    UserCreated(
                        name = command.name,
                        accountId = command.accountId,
                        primaryEmail = command.primaryEmail,
                        primaryPhone = command.primaryPhone,
                        avatar = command.avatar,
                        username = command.username,
                    )
                }
            )
    }

    @OnCommand
    fun onUpdate(command: UpdateUser): UserUpdated {
        require(state.name != null) { "用户不存在" }
        return UserUpdated(
            name = command.name,
            primaryEmail = command.primaryEmail,
            primaryPhone = command.primaryPhone,
            avatar = command.avatar
        )
    }

    @OnCommand
    fun onDelete(command: DeleteUser): UserDeleted {
        require(state.status != UserStatus.DISABLED) { "用户已被禁用" }
        return UserDeleted(
            reason = command.reason
        )
    }

    @OnCommand
    fun onChangePassword(
        command: ChangeUserPassword,
        passwordEncoder: PasswordEncoder
    ): Mono<UserPasswordChanged> {
        require(state.status == UserStatus.ACTIVE) { "用户状态不允许修改密码" }

        require(passwordEncoder.matches(command.oldPassword, state.passwordEncrypted)) {
            "密码错误"
        }

        val passwordEncrypted = passwordEncoder.encode(command.newPassword)

        return UserPasswordChanged(
            encryptedPassword = passwordEncrypted,
            encryptionMethod = "BCRYPT"
        ).toMono()
    }

    @OnCommand
    fun onUpdateStatus(command: UpdateUserStatus): UserStatusUpdated {
        require(state.status != command.status) { "用户已经处于${command.status}状态" }

        return UserStatusUpdated(
            status = command.status,
            reason = command.reason
        )
    }

//    @OnCommand
//    fun onUpdateIdentities(command: UpdateUserIdentities): UserIdentitiesUpdated {
//        bindPrepare(state.status == UserStatus.ACTIVE) { "用户状态不允许更新身份信息" }
//
//        return UserIdentitiesUpdated(
//            identities = command.identities
//        )
//    }

    @OnCommand
    fun onUpdateProfile(command: UpdateUserProfile): UserProfileUpdated {
        require(state.status == UserStatus.ACTIVE) { "用户状态不允许更新档案信息" }

        return UserProfileUpdated(
            nickname = command.nickname,
            profile = command.profile,
            website = command.website,
            gender = command.gender,
            birthdate = command.birthdate,
            locale = command.locale,
            address = command.address
        )
    }

    @OnCommand
    fun onUpdateCustomData(command: UpdateUserCustomData): UserCustomDataUpdated {
        require(state.status == UserStatus.ACTIVE) { "用户状态不允许更新自定义数据" }

        return UserCustomDataUpdated(
            customData = command.customData
        )
    }

    @OnError
    fun onError(command: Any, error: Throwable): Mono<Void> {
        // TODO: 实现错误处理逻辑
        return Mono.empty()
    }
}
