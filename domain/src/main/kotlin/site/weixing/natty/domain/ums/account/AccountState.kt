package site.weixing.natty.domain.ums.account

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.ums.account.AccountCreated
import site.weixing.natty.api.ums.account.AccountDeleted
import site.weixing.natty.api.ums.account.AccountLocked
import site.weixing.natty.api.ums.account.AccountUnlocked
import site.weixing.natty.api.ums.account.AccountUpdated
import site.weixing.natty.api.ums.account.RoleAssigned

class AccountState(override val id: String) : Identifier {
    var userId: String? = null
        private set
    var username: String? = null
        private set
    var email: String? = null
        private set
    var phone: String? = null
        private set
    var status: AccountStatus = AccountStatus.ACTIVE
        private set
    var roleIds: Set<String> = emptySet()
        private set

    @OnSourcing
    fun onCreated(event: AccountCreated) {
        username = event.username
        email = event.email
        phone = event.phone
    }

    @OnSourcing
    fun onUpdated(event: AccountUpdated) {
        username = event.username
        email = event.email
        phone = event.phone
    }

    @OnSourcing
    fun onLocked(event: AccountLocked) {
        status = AccountStatus.LOCKED
    }

    @OnSourcing
    fun onUnlocked(event: AccountUnlocked) {
        status = AccountStatus.ACTIVE
    }

    @OnSourcing
    fun onDeleted(event: AccountDeleted) {
        status = AccountStatus.DISABLED
    }

    @OnSourcing
    fun onRoleAssigned(event: RoleAssigned) {
        roleIds = event.roleIds
    }
}

enum class AccountStatus {
    ACTIVE,
    LOCKED,
    DISABLED
}
