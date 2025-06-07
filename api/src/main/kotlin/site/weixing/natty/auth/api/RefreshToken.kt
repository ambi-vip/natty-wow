package site.weixing.natty.auth.api

import java.time.Instant

/**
 * Desc
 * @author ambi
 */

data class RefreshToken(
    val refreshToken: String
)

/**
 * 令牌刷新事件
 */
data class TokenRefreshed(
    val accountId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant
)
