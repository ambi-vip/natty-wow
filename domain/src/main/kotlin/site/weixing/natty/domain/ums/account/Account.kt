package site.weixing.natty.domain.ums.account

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.ums.account.AccountCreated
import site.weixing.natty.api.ums.account.AccountDeleted
import site.weixing.natty.api.ums.account.AccountLocked
import site.weixing.natty.api.ums.account.AccountUnlocked
import site.weixing.natty.api.ums.account.AccountUpdated
import site.weixing.natty.api.ums.account.AssignRole
import site.weixing.natty.api.ums.account.CreateAccount
import site.weixing.natty.api.ums.account.DeleteAccount
import site.weixing.natty.api.ums.account.LockAccount
import site.weixing.natty.api.ums.account.RoleAssigned
import site.weixing.natty.api.ums.account.UnlockAccount
import site.weixing.natty.api.ums.account.UpdateAccount

@Suppress("unused")
@AggregateRoot
class Account(private val state: AccountState) {

    @OnCommand
    fun onCreate(command: CreateAccount,
                 usernamePrepare: UsernamePrepare
                 ): Mono<AccountCreated> {
        return usernamePrepare.usingPrepare(
            key = command.username,
            value = UsernameIndexValue(
                userId = state.id,
                password = "encodedPassword",
            ),
        ) {
            require(it) {
                "username[${command.username}] is already registered."
            }
            AccountCreated(
                username = command.username,
                email = command.email,
                phone = command.phone
            ).toMono()
        }
    }

    @OnError
    fun onError(createAccount: CreateAccount, error: Throwable): Mono<Void> {
        // 自定义错误处理逻辑
        // 可以记录日志、发送通知、发送失败时间 补偿 saga 等
        return Mono.empty()
    }

    @OnCommand
    fun onUpdate(command: UpdateAccount): AccountUpdated {
        return AccountUpdated(
            accountId = command.id,
            username = command.username,
            email = command.email,
            phone = command.phone
        )
    }

    @OnCommand
    fun onLock(command: LockAccount): AccountLocked {
        if (state.status == AccountStatus.LOCKED) {
            throw IllegalStateException("Account is already locked")
        }
        return AccountLocked(
            accountId = command.id,
            reason = command.reason
        )
    }

    @OnCommand
    fun onUnlock(command: UnlockAccount): AccountUnlocked {
        if (state.status != AccountStatus.LOCKED) {
            throw IllegalStateException("Account is not locked")
        }
        return AccountUnlocked(
            accountId = command.id
        )
    }

    @OnCommand
    fun onDelete(command: DeleteAccount): AccountDeleted {
        if (state.status == AccountStatus.DISABLED) {
            throw IllegalStateException("Account is already disabled")
        }
        return AccountDeleted(
            accountId = command.id
        )
    }

    @OnCommand
    fun onAssignRole(command: AssignRole): RoleAssigned {
        if (state.status != AccountStatus.ACTIVE) {
            throw IllegalStateException("Cannot assign roles to non-active account")
        }
        return RoleAssigned(
            roleIds = command.roleIds
        )
    }
}
