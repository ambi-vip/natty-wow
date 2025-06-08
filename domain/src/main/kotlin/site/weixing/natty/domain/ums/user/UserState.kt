package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.ums.api.user.UserCreated

class UserState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var accountId: String? = null
        private set
    var email: String? = null
        private set
    var phone: String? = null
        private set
    var avatar: String? = null
        private set
    var status: UserStatus = UserStatus.ACTIVE
        private set
    var deptCode: String? = null
        private set

    @OnSourcing
    fun onCreated(event: UserCreated) {
        name = event.name
        accountId = event.accountId
        email = event.email
        phone = event.phone
        avatar = event.avatar
    }

//    @OnSourcing
//    fun onUpdated(event: UserUpdated) {
//        name = event.name
//        email = event.email
//        phone = event.phone
//        avatar = event.avatar
//    }
//
//    @OnSourcing
//    fun onDeleted(event: UserDeleted) {
//        status = UserStatus.DISABLED
//    }
}

enum class UserStatus {
    ACTIVE,
    DISABLED
}
