package site.weixing.natty.auth.domain

import reactor.core.publisher.Mono
import site.weixing.natty.auth.api.AuthAuthenticated
import site.weixing.natty.auth.api.AuthInvalidated
import site.weixing.natty.auth.api.Authenticate
import site.weixing.natty.auth.api.Logout
import site.weixing.natty.auth.api.RefreshToken

/**
 * 认证处理器
 * @author ambi
 */
interface AuthHandler{

    fun authenticate(command: Authenticate): Mono<AuthAuthenticated>

    fun refreshToken(command: RefreshToken): Mono<AuthAuthenticated>

    fun logout(command: Logout): Mono<AuthInvalidated>

}

