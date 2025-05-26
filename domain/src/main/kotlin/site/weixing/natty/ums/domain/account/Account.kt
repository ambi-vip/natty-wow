package site.weixing.natty.ums.domain.account

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.ums.api.account.AccountCreated
import site.weixing.natty.ums.api.account.AccountDeleted
import site.weixing.natty.ums.api.account.AccountLocked
import site.weixing.natty.ums.api.account.AccountUnlocked
import site.weixing.natty.ums.api.account.AccountUpdated
import site.weixing.natty.ums.api.account.AssignRole
import site.weixing.natty.ums.api.account.CreateAccount
import site.weixing.natty.ums.api.account.DeleteAccount
import site.weixing.natty.ums.api.account.LockAccount
import site.weixing.natty.ums.api.account.RoleAssigned
import site.weixing.natty.ums.api.account.UnlockAccount
import site.weixing.natty.ums.api.account.UpdateAccount

@Suppress("unused")
@AggregateRoot
class Account(private val state: AccountState) {

    @OnCommand
    fun onCreate(command: CreateAccount): AccountCreated {
        return AccountCreated(
            username = command.username,
            email = command.email,
            phone = command.phone
        )
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
