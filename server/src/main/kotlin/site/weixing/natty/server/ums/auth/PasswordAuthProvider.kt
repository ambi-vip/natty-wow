package site.weixing.natty.server.ums.auth

import me.ahoo.cosec.api.authentication.Authentication
import me.ahoo.cosec.api.principal.CoSecPrincipal
import me.ahoo.cosec.principal.SimplePrincipal
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import site.weixing.natty.auth.PasswordCredentialsToken
import site.weixing.natty.domain.ums.account.UsernamePrepare
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder
import site.weixing.natty.server.ums.user.UserService
import java.lang.RuntimeException

// 认证异常
class AuthException(message: String) : RuntimeException(message)

interface AccountAuthStrategy {
    val accountType: String
    fun authenticate(username: String, password: String): Mono<CoSecPrincipal>
}

@Component
class AdminAuthStrategy(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val usernamePrepare: UsernamePrepare
) : AccountAuthStrategy {
    override val accountType: String = "ADMIN"

    override fun authenticate(username: String, password: String): Mono<CoSecPrincipal> {
        return usernamePrepare.get(username)
            .filter { passwordEncoder.matches(password, it.password) }
            .switchIfEmpty(Mono.error(AuthException("认证失败")))
            .flatMap { userService.getById(it.userId) }
            .map { user ->
                SimplePrincipal(
                    id = user.id,
                    attributes = mapOf(
                        "username" to (user.name ?: ""),
                        "primaryPhone" to (user.primaryPhone ?: ""),
                        "primaryEmail" to (user.primaryEmail ?: ""),
                        "accountType" to accountType
                    )
                )
            }
    }
}

@Component
class PasswordAuthProvider(
    strategies: List<AccountAuthStrategy>
) : Authentication<PasswordCredentialsToken, CoSecPrincipal> {

    private val strategyMap = strategies.associateBy { it.accountType }

    override val supportCredentials: Class<PasswordCredentialsToken>
        get() = PasswordCredentialsToken::class.java

    override fun authenticate(credentials: PasswordCredentialsToken): Mono<CoSecPrincipal> {
        val strategy = strategyMap[credentials.accountType]
            ?: return Mono.error(AuthException("不支持的账户类型: ${credentials.accountType}"))
        return strategy.authenticate(credentials.username, credentials.password)
            .doOnError { ex ->
                // 认证失败日志
                // log.warn("认证失败: type=${credentials.accountType}, username=${credentials.username}, reason=${ex.message}")
            }
    }
}
