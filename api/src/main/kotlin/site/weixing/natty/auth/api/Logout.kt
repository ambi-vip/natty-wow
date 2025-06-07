package site.weixing.natty.auth.api

/**
 * Desc
 * @author ambi
 */

data class Logout(
    val accountId: String

)

/**
 * 认证失效事件
 */
data class AuthInvalidated(
    val accountId: String,
    val reason: String,
    val accountType: String,
)