package site.weixing.natty.domain.auth

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.auth.AuthInvalidated
import site.weixing.natty.api.auth.AuthSucceeded
import site.weixing.natty.api.auth.GrantType
import site.weixing.natty.api.auth.TokenRefreshed
import java.time.Instant

/**
 * 认证会话状态
 * @author ambi
 */
class AuthSessionState(override val id: String) : Identifier {

    var accountId: String? = null
        private set

    var accountType: String? = null
        private set

    var grantType: GrantType? = null
        private set

    var accessToken: String? = null
        private set

    var refreshToken: String? = null
        private set

    var expiresAt: Instant? = null
        private set

    var isActive: Boolean = true
        private set

    @OnSourcing
    fun onAuthSucceeded(event: AuthSucceeded) {
        this.accountId = event.accountId
        this.accountType = event.accountType
        this.grantType = event.grantType
        this.accessToken = event.accessToken
        this.refreshToken = event.refreshToken
        this.expiresAt = event.expiresAt
        this.isActive = true
    }

    @OnSourcing
    fun onTokenRefreshed(event: TokenRefreshed) {
        this.accessToken = event.accessToken
        this.refreshToken = event.refreshToken
        this.expiresAt = event.expiresAt
    }

    @OnSourcing
    fun onAuthInvalidated(event: AuthInvalidated) {
        this.isActive = false
        this.accessToken = null
        this.refreshToken = null
        this.expiresAt = null
    }

    fun isExpired(): Boolean {
        return expiresAt?.isBefore(Instant.now()) ?: true
    }
}
