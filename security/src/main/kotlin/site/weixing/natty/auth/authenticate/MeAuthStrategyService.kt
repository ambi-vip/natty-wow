package site.weixing.natty.auth.authenticate

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import site.weixing.natty.auth.api.AuthAuthenticated
import site.weixing.natty.auth.api.Authenticate
import site.weixing.natty.auth.api.GrantType.*
import site.weixing.natty.auth.authorization.CoCredentialsToken
import site.weixing.natty.auth.domain.AuthHandler
import site.weixing.natty.auth.domain.commands.AuthenticationManager
import java.lang.IllegalArgumentException

/**
 * Desc
 * @author ambi
 */
//@Service
//class MeAuthStrategyService(
//    private val authenticationManager: AuthenticationManager
//) : AuthenticationManager {
//
//    override fun authenticate(authenticate: Authenticate): Mono<AuthAuthenticated> {
//
//        // 第一步转 token 第二步开始登录
//
//        val credentials = when (authenticate.grantType) {
//            PASSWORD -> {
//                UsernamePasswordCredentials(
//                    username = authenticate.credentials["username"] as String,
//                    password = authenticate.credentials["password"] as String,
//                    accountType = authenticate.accountType
//                )
//            }
//
//            AUTHORIZATION_CODE -> throw IllegalArgumentException("暂不支持")
//            IMPLICIT -> throw IllegalArgumentException("暂不支持")
//            REFRESH_TOKEN -> throw IllegalArgumentException("暂不支持")
//            CLIENT_CREDENTIALS -> throw IllegalArgumentException("暂不支持")
//        }
//
//        return authenticationManager.authenticate(credentials)
//            .map {
//                AuthAuthenticated(
//                    accountId = it.accountId,
//                    accountType = it.accessToken,
//                    grantType = authenticate.grantType,
//                    accessToken = it.accessToken,
//                    refreshToken = it.refreshToken,
//                    expiresIn = 1
//                )
//            };
//    }
//}
//
//data class UsernamePasswordCredentials(
//    val username: String,
//    val password: String,
//    override var accountType: String
//) : CoCredentialsToken()