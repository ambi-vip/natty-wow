package site.weixing.natty.auth.domain.commands

import reactor.core.publisher.Mono

/**
 * Desc
 * @author ambi
 */
interface AuthenticationManager {

    fun authenticate(credentials: CredentialsToken): Mono<out AuthenticatedToken>

}


/**
 * Desc
 * @author ambi
 */
abstract class CredentialsToken


abstract class AuthenticatedToken {
    abstract val accessToken : String
    abstract val refreshToken: String
    abstract val accountId: String
}