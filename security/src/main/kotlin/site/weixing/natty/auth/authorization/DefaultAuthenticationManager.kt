package site.weixing.natty.auth.authorization

import me.ahoo.cosec.api.authentication.Credentials
import me.ahoo.cosec.api.token.CompositeToken
import me.ahoo.cosec.authentication.CompositeAuthentication
import me.ahoo.cosec.token.TokenCompositeAuthentication
import me.ahoo.cosec.token.TokenConverter
import reactor.core.publisher.Mono
import site.weixing.natty.auth.domain.commands.AuthenticatedToken
import site.weixing.natty.auth.domain.commands.AuthenticationManager
import site.weixing.natty.auth.domain.commands.CredentialsToken

/**
 * Desc
 * @author ambi
 */
class DefaultAuthenticationManager(
    private val compositeAuthentication: CompositeAuthentication,
    private val tokenConverter: TokenConverter
) : AuthenticationManager {

    override fun authenticate(credentials: CredentialsToken): Mono<out AuthenticatedToken> {
        return compositeAuthentication.authenticate(credentials as CoCredentialsToken)
            .map {
                val toToken = tokenConverter.toToken(it)
                SimpleCompositeToken(toToken.accessToken, toToken.refreshToken, it.id)
            }
    }
}

abstract class CoCredentialsToken : CredentialsToken(), Credentials {
    abstract var accountType: String
}

abstract class CoAuthenticatedToken : AuthenticatedToken(), CompositeToken

data class SimpleCompositeToken(
    override val accessToken: String,
    override val refreshToken: String,
    override val accountId: String,
) : CoAuthenticatedToken()