package site.weixing.natty.api.ums.user

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "profile",
    summary = "更新用户档案信息"
)
data class UpdateUserProfile(

    val nickname: String? = null,
    val profile: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val locale: String? = null,
    val address: Address? = null

)

data class Address(
    val formatted: String? = null,
    val streetAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

data class UserProfileUpdated(
    val familyName: String? = null,
    val givenName: String? = null,
    val middleName: String? = null,
    val nickname: String? = null,
    val preferredUsername: String? = null,
    val profile: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val locale: String? = null,
    val address: Address? = null,
    val updatedAt: Long = System.currentTimeMillis()
) 
