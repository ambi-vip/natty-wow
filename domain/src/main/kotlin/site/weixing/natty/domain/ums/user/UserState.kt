package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.ums.user.UserStatus
import site.weixing.natty.ums.api.user.UserCreated
import site.weixing.natty.api.ums.user.UserCustomDataUpdated
import site.weixing.natty.api.ums.user.UserDeleted
//import site.weixing.natty.api.ums.user.UserIdentitiesUpdated
import site.weixing.natty.ums.api.user.UserPasswordChanged
import site.weixing.natty.api.ums.user.UserProfileUpdated
import site.weixing.natty.api.ums.user.UserStatusUpdated
import site.weixing.natty.api.ums.user.UserUpdated

class UserState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var applicationId: String? = null
        private set
    var accountId: String? = null
        private set

    //    union
    var username: String? = null
        private set

    //    union
    var primaryEmail: String? = null
        private set

    //    union;I18n
    var primaryPhone: String? = null
        private set
    var avatar: String? = null
        private set

    //  require
    var status: UserStatus = UserStatus.ACTIVE
        private set
    var deptCode: String? = null
        private set

    var customData: Map<String, Any>? = null
        private set

    //  require
    var identities: MutableMap<String, UserIdentity> = mutableMapOf()
        private set

    //  require
    var profile: UserProfile = UserProfile()
        private set

    //  invisible
    var passwordEncrypted: String? = null
        private set

    //  invisible
    var passwordEncryptionMethod: String? = null
        private set
    var lastSignInAt: Long? = null
        private set


    @OnSourcing
    fun onCreated(event: UserCreated) {
        name = event.name
        accountId = event.accountId
        primaryEmail = event.email
        primaryPhone = event.phone
        avatar = event.avatar
    }

    @OnSourcing
    fun onUpdated(event: UserUpdated) {
        event.name?.let { name = it }
        event.email?.let { primaryEmail = it }
        event.phone?.let { primaryPhone = it }
        event.avatar?.let { avatar = it }
    }

    @OnSourcing
    fun onDeleted(event: UserDeleted) {
        status = UserStatus.DISABLED
    }

    @OnSourcing
    fun onPasswordChanged(event: UserPasswordChanged) {
        passwordEncrypted = event.encryptedPassword
        passwordEncryptionMethod = event.encryptionMethod
    }

    @OnSourcing
    fun onStatusUpdated(event: UserStatusUpdated) {
        status = event.status
    }

//    @OnSourcing
//    fun onIdentitiesUpdated(event: UserIdentitiesUpdated) {
////        identities = event.identities
////        identities.pu
//    }



    @OnSourcing
    fun onProfileUpdated(event: UserProfileUpdated) {
        profile = UserProfile(
            nickname = event.nickname,
            profile = event.profile,
            website = event.website,
            gender = event.gender,
            birthdate = event.birthdate,
            locale = event.locale,
            address = Address(
                formatted = event.address?.formatted,
                streetAddress = event.address?.streetAddress,
                locality = event.address?.locality,
                region = event.address?.region,
                postalCode = event.address?.postalCode,
                country = event.address?.country,
            )
        )
    }

    @OnSourcing
    fun onCustomDataUpdated(event: UserCustomDataUpdated) {
        customData = event.customData
    }
}
