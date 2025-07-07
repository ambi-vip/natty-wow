package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import me.ahoo.wow.api.command.CommandResultAccessor
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.ums.user.ChangeUserPrimaryEmail
import site.weixing.natty.api.ums.user.ChangeUserPrimaryPhone
import site.weixing.natty.api.ums.user.DeleteUser
import site.weixing.natty.api.ums.user.UpdateUser
import site.weixing.natty.api.ums.user.UpdateUserCustomData
// import site.weixing.natty.api.ums.user.UpdateUserIdentities
import site.weixing.natty.api.ums.user.UpdateUserProfile
import site.weixing.natty.api.ums.user.UpdateUserStatus
import site.weixing.natty.api.ums.user.UserCustomDataUpdated
import site.weixing.natty.api.ums.user.UserDeleted
// import site.weixing.natty.api.ums.user.UserIdentitiesUpdated
import site.weixing.natty.api.ums.user.UserProfileUpdated
import site.weixing.natty.api.ums.user.UserStatus
import site.weixing.natty.api.ums.user.UserStatusUpdated
import site.weixing.natty.api.ums.user.UserUpdated
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder
import site.weixing.natty.ums.api.user.ChangeUserPassword
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated
import site.weixing.natty.api.ums.user.UserPrimaryEmailChanged
import site.weixing.natty.api.ums.user.UserPrimaryPhoneChanged
import site.weixing.natty.domain.ums.account.UsernameIndexValue
import site.weixing.natty.domain.ums.account.UsernamePrepare
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
            .flatMap { usernameIndexValue ->
                Mono.fromCallable {
                    UserCreated(
                        name = command.name,
                        accountId = command.accountId,
                        primaryEmail = command.primaryEmail,
                        primaryPhone = command.primaryPhone,
                        avatar = command.avatar,
                        username = command.username,
                        passwordEncrypted = usernameIndexValue?.password,
                        passwordEncryptionMethod = usernameIndexValue?.encryptionMethod
                    )
                }
            }
    }

    @OnCommand
    fun onUpdate(
        command: UpdateUser
    ): Mono<UserUpdated> {
        return UserUpdated(
            name = command.name,
            avatar = command.avatar
        ).toMono()
    }

    @OnCommand
    fun onChangePrimalPhone(
        command: ChangeUserPrimaryPhone,
        usernamePrepare: UsernamePrepare,
    ): Mono<UserPrimaryPhoneChanged> {
        require(state.status == UserStatus.ACTIVE) { "用户状态不允许修改手机号" }

        val usernameIndexValue = UsernameIndexValue(
            userId = state.id,
            password = state.passwordEncrypted,
            encryptionMethod = state.passwordEncryptionMethod
        )

        return if (state.primaryPhone.isNullOrBlank()) {
            // 用户没有手机号，直接准备新手机号
            usernamePrepare.usingPrepare(
                key = command.newPhone,
                value = usernameIndexValue,
            ) { success ->
                require(success) { "手机号${command.newPhone}已被占用" }
                UserPrimaryPhoneChanged(
                    oldPhone = null,
                    newPhone = command.newPhone,
                ).toMono()
            }
        } else {
            // 用户已有手机号，使用 reprepare 替换
            usernamePrepare.reprepare(
                oldKey = state.primaryPhone!!,
                oldValue = usernameIndexValue,
                newKey = command.newPhone,
                newValue = usernameIndexValue
            ).map { success ->
                require(success) { "手机号${command.newPhone}已被占用" }
                UserPrimaryPhoneChanged(
                    oldPhone = state.primaryPhone,
                    newPhone = command.newPhone,
                )
            }
        }
    }

    @OnCommand
    fun onChangePrimalEmail(
        command: ChangeUserPrimaryEmail,
        usernamePrepare: UsernamePrepare,
    ): Mono<UserPrimaryEmailChanged> {
        require(state.status == UserStatus.ACTIVE) { "用户状态不允许修改邮箱" }

        val usernameIndexValue = UsernameIndexValue(
            userId = state.id,
            password = state.passwordEncrypted,
            encryptionMethod = state.passwordEncryptionMethod
        )

        return if (state.primaryPhone.isNullOrBlank()) {
            // 用户没有邮箱，直接准备新邮箱
            usernamePrepare.usingPrepare(
                key = command.newEmail,
                value = usernameIndexValue,
            ) { success ->
                require(success) { "邮箱${command.newEmail}已被占用" }
                UserPrimaryEmailChanged(
                    oldEmail = null,
                    newEmail = command.newEmail,
                ).toMono()
            }
        } else {
            // 用户已有邮箱，使用 reprepare 替换
            usernamePrepare.reprepare(
                oldKey = state.primaryPhone!!,
                oldValue = usernameIndexValue,
                newKey = command.newEmail,
                newValue = usernameIndexValue
            ).map { success ->
                require(success) { "邮箱${command.newEmail}已被占用" }
                UserPrimaryEmailChanged(
                    oldEmail = state.primaryEmail,
                    newEmail = command.newEmail,
                )
            }
        }
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

    @OnCommand
    fun onDelete(command: DeleteUser): UserDeleted {
        require(state.status != UserStatus.DISABLED) { "用户已被禁用" }
        return UserDeleted(
            reason = command.reason
        )
    }

    @OnError
    fun onError(command: Any, error: Throwable): Mono<Void> {
        // TODO: 实现错误处理逻辑
        return Mono.empty()
    }
}
