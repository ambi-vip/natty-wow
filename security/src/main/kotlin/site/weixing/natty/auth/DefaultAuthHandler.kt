package site.weixing.natty.auth

import reactor.core.publisher.Mono
import site.weixing.natty.api.auth.AuthAuthenticated
import site.weixing.natty.api.auth.AuthInvalidated
import site.weixing.natty.api.auth.Authenticate
import site.weixing.natty.api.auth.GrantType
import site.weixing.natty.api.auth.Logout
import site.weixing.natty.api.auth.RefreshToken
import site.weixing.natty.auth.authorization.CoCredentialsToken
import site.weixing.natty.auth.authorization.SimpleCompositeToken
import site.weixing.natty.domain.auth.AuthHandler
import site.weixing.natty.domain.auth.commands.AuthenticationManager
import site.weixing.natty.domain.auth.commands.CredentialsToken

/**
 * Desc
 * @author ambi
 */
class DefaultAuthHandler(
    private val authenticationManager: AuthenticationManager
) : AuthHandler {

    override fun authenticate(command: Authenticate): Mono<AuthAuthenticated> {
        val credentials = createCredentialsToken(command)
        return authenticationManager.authenticate(credentials)
            .handle { token, sink ->
                when (token) {
                    is SimpleCompositeToken -> sink.next(
                        AuthAuthenticated(
                            accountId = token.accountId,
                            accountType = command.accountType,
                            grantType = command.grantType,
                            accessToken = token.accessToken,
                            refreshToken = token.refreshToken,
                            expiresIn = 3600L // 默认1小时过期
                        )
                    )
                    else -> sink.error(IllegalStateException("不支持的令牌类型"))
                }
            }
    }


    private fun createCredentialsToken(command: Authenticate): CredentialsToken {
        // 根据不同的认证类型创建对应的凭证令牌
        return when (command.grantType) {
            GrantType.PASSWORD -> PasswordCredentialsToken(
                username = command.credentials["username"] as String,
                password = command.credentials["password"] as String,
                accountType = command.accountType
            )

            GrantType.REFRESH_TOKEN -> RefreshTokenCredentialsToken(
                refreshToken = command.credentials["refreshToken"] as String,
                accountType = command.accountType
            )

            else -> throw IllegalArgumentException("不支持的授权类型: ${command.grantType}")
        }
    }

    override fun refreshToken(command: RefreshToken): Mono<AuthAuthenticated> {
        // 实现刷新令牌的逻辑
        return Mono.error(UnsupportedOperationException("刷新令牌功能尚未实现"))
    }

    override fun logout(command: Logout): Mono<AuthInvalidated> {
        // 实现登出逻辑
        return Mono.empty()
    }

}

data class PasswordCredentialsToken(
    val username: String,
    val password: String,
    override var accountType: String
) : CoCredentialsToken()

data class RefreshTokenCredentialsToken(
    val refreshToken: String,
    override var accountType: String
) : CoCredentialsToken()