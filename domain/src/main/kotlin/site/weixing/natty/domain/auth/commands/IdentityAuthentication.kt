package site.weixing.natty.domain.auth.commands

import reactor.core.publisher.Mono
import site.weixing.natty.api.auth.GrantType

/**
 * 身份认证抽象方法
 * @author ambi
 */
interface IdentityAuthentication {

    val accountType: String
    val grantType: GrantType

    fun authenticate(authenticate: CredentialsToken): Mono<AuthenticatedToken>

}