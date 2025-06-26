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

/**
 * 用户认证提供者
 * 支持基于 accountType 的不同账户体系
 * @author ambi
 */
@Component
class PasswordAuthProvider(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val usernamePrepare: UsernamePrepare
) : Authentication<PasswordCredentialsToken, CoSecPrincipal> {

    override val supportCredentials: Class<PasswordCredentialsToken>
        get() = PasswordCredentialsToken::class.java

    override fun authenticate(credentials: PasswordCredentialsToken): Mono<CoSecPrincipal> {
        return when (credentials.accountType) {
            "ADMIN" -> validateAdminUser(credentials.username, credentials.password)
//            "USER" -> validateNormalUser(credentials.username, credentials.password)
            else -> Mono.error(IllegalArgumentException("不支持的账户类型: ${credentials.accountType}"))
        }
    }

    private fun validateAdminUser(username: String, password: String): Mono<CoSecPrincipal> {
        return usernamePrepare.get(username)
            .filter { passwordEncoder.matches(password, it.password) }
            .switchIfEmpty(Mono.error(RuntimeException("密码不正确")))
            .flatMap { userService.getById(it.userId) }
            .map { user ->
                SimplePrincipal(
                    id = user.id,
                    attributes = mapOf(
                        "username" to (user.name ?: ""),
                        "primaryPhone" to (user.primaryPhone ?: ""),
                        "primaryEmail" to (user.primaryEmail ?: ""),
                        "accountType" to "ADMIN"
                    )
                )
            }
    }
}
