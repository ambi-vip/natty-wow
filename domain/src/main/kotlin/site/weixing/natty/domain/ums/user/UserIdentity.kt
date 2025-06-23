package site.weixing.natty.domain.ums.user

/**
 * UserIdentity
 * @author ambi
 */
data class UserIdentity(
    val openId: String,
    val detail: UserIdentityDetail
)

data class UserIdentityDetail(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
)