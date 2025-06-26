package site.weixing.natty.domain.ums.user

/**
 * Desc
 * @author ambi
 */
data class UserProfile(
    val nickname: String? = null,
    val profile: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val locale: String? = null,
    val address: Address = Address()
)

data class Address(
    val formatted: String? = null,
    val streetAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)
