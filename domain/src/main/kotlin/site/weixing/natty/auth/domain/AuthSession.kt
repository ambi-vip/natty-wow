package site.weixing.natty.auth.domain

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.AggregateRoute
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.api.command.CommandResultAccessor
import me.ahoo.wow.event.DomainEventStream
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.auth.api.AuthAuthenticated
import site.weixing.natty.auth.api.AuthError
import site.weixing.natty.auth.api.AuthInvalidated
import site.weixing.natty.auth.api.Authenticate
import site.weixing.natty.auth.api.Logout
import site.weixing.natty.auth.api.RefreshToken
import java.time.Instant

/**
 * 认证会话聚合根
 * @author ambi
 */
@AggregateRoot
@StaticTenantId
@AggregateRoute(resourceName = "auth")
class AuthSession(
    private val state: AuthSessionState,
) {

    companion object {
        private val log = LoggerFactory.getLogger(AuthSession::class.java)
    }

    @OnCommand
    fun onLogin(
        command: Authenticate,
        handle: AuthHandler,
        commandResultAccessor: CommandResultAccessor,
    ): Mono<AuthAuthenticated> {
        return handle.authenticate(command)
            .doOnNext {
                commandResultAccessor.setCommandResult(AuthAuthenticated::accessToken.name, it.accessToken)
                commandResultAccessor.setCommandResult(AuthAuthenticated::refreshToken.name, it.refreshToken)
            }
    }


    @OnCommand
    fun onRefreshToken(
        command: RefreshToken,
        handle: AuthHandler,
        commandResultAccessor: CommandResultAccessor
    ): Mono<AuthAuthenticated> {
        return handle.refreshToken(command)
            .doOnNext {
                commandResultAccessor.setCommandResult(AuthAuthenticated::accessToken.name, it.accessToken)
                commandResultAccessor.setCommandResult(AuthAuthenticated::refreshToken.name, it.refreshToken)
            }
    }

    @OnCommand
    fun onLogout(
        command: Logout,
        handle: AuthHandler
    ): Mono<AuthInvalidated> {
        return handle.logout(command)
    }

    fun isActive(): Boolean = state.isActive && !state.isExpired()
}

