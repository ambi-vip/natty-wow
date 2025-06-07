package site.weixing.natty.auth.api

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import java.time.Instant

/**
 * Desc
 * @author ambi
 */
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/authenticate",
    summary = "用户认证"
)
data class Authenticate(
    val grantType: GrantType,
    val accountType: String,
    val credentials: Map<String, Any>
)

data class AuthAuthenticated(
    val accountId: String,
    val accountType: String,
    val grantType: GrantType,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

/**
 * 认证成功事件
 */
data class AuthSucceeded(
    val accountId: String,
    val accountType: String,
    val grantType: GrantType,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant
)

/**
 * 认证成功事件
 */
data class AuthError(
    val accountType: String,
    val grantType: GrantType,
    val reason: String,
    val errorAt: Instant
)


enum class GrantType {

    AUTHORIZATION_CODE,
    IMPLICIT,
    REFRESH_TOKEN,
    PASSWORD,
    CLIENT_CREDENTIALS,


}
