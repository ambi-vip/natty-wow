package site.weixing.natty.domain.auth

import reactor.core.publisher.Mono
import site.weixing.natty.api.auth.AuthAuthenticated
import site.weixing.natty.api.auth.AuthInvalidated
import site.weixing.natty.api.auth.Authenticate
import site.weixing.natty.api.auth.Logout
import site.weixing.natty.api.auth.RefreshToken

/**
 * 认证处理器
 * @author ambi
 */
interface AuthHandler {

    fun authenticate(command: Authenticate): Mono<AuthAuthenticated>

    fun refreshToken(command: RefreshToken): Mono<AuthAuthenticated>

    fun logout(command: Logout): Mono<AuthInvalidated>
}
