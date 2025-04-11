package site.weixing.natty.domain.platform.ums.user

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.platform.api.ums.UserCreated
import site.weixing.natty.platform.api.ums.UserStatus
import site.weixing.natty.platform.api.ums.UserUpdated

class UserState(override val id: String) : Identifier {
    var username: String? = null
        private set

    var email: String? = null
        private set

    var phone: String? = null
        private set

    var nickname: String? = null
        private set

    var password: String? = null
        private set

    var status: UserStatus = UserStatus.ENABLED
        private set

    var roleIds: List<String> = emptyList()
        private set

    @OnSourcing
    fun onCreated(event: UserCreated) {
        username = event.username
        email = event.email
        phone = event.phone
        nickname = event.nickname
        status = event.status
        roleIds = event.roleIds
    }

    @OnSourcing
    fun onUpdated(event: UserUpdated) {
        event.email?.let { email = it }
        event.phone?.let { phone = it }
        event.nickname?.let { nickname = it }
        event.status?.let { status = it }
        event.roleIds?.let { roleIds = it }
    }
}
