package site.weixing.natty.server.ums.auth

import me.ahoo.cosec.api.authentication.Authentication
import me.ahoo.cosec.api.principal.CoSecPrincipal
import me.ahoo.cosec.principal.SimplePrincipal
import me.ahoo.wow.exception.throwNotFoundIfEmpty
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.auth.PasswordCredentialsToken
import site.weixing.natty.ums.domain.account.AccountState
import site.weixing.natty.ums.domain.user.UserState
import site.weixing.natty.ums.domain.user.UserStateProperties

/**
 * 用户认证提供者
 * 支持基于 accountType 的不同账户体系
 * @author ambi
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class UserAuthProvider(
    private val accountQueryService: SnapshotQueryService<AccountState>,
    private val userQueryService: SnapshotQueryService<UserState>
) : Authentication<PasswordCredentialsToken, CoSecPrincipal> {

    override val supportCredentials: Class<PasswordCredentialsToken>
        get() = PasswordCredentialsToken::class.java

    override fun authenticate(credentials: PasswordCredentialsToken): Mono<CoSecPrincipal> {
        return when (credentials.accountType) {
            "ADMIN" -> validateAdminUser(credentials.username, credentials.password)
            "USER" -> validateNormalUser(credentials.username, credentials.password)
            else -> Mono.error(IllegalArgumentException("不支持的账户类型: ${credentials.accountType}"))
        }
    }

    private fun validateAdminUser(username: String, password: String): Mono<CoSecPrincipal> {
        return singleQuery {
            condition {
                nestedState()
                UserStateProperties.NAME eq username
            }
        }.query(userQueryService)
            .toState()
            .throwNotFoundIfEmpty()
            .map { user ->
                SimplePrincipal(
                    id = user.id,
                    attributes = mapOf(
                        "username" to (user.name ?: ""),
                        "phone" to (user.phone ?: ""),
                        "email" to (user.email ?: ""),
                        "accountType" to "ADMIN"
                    )
                )
            }
    }

    private fun validateNormalUser(username: String, password: String): Mono<CoSecPrincipal> {
        return singleQuery {
            condition {
                nestedState()
                UserStateProperties.NAME eq username
            }
        }.query(userQueryService)
            .toState()
            .throwNotFoundIfEmpty()
            .map { user ->
                SimplePrincipal(
                    id = user.id,
                    attributes = mapOf(
                        "username" to (user.name ?: ""),
                        "phone" to (user.phone ?: ""),
                        "email" to (user.email ?: ""),
                        "accountType" to "USER"
                    )
                )
            }
    }
}

