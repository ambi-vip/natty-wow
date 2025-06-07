package site.weixing.natty.auth.authenticate

import me.ahoo.cosec.api.authentication.Authentication
import me.ahoo.cosec.api.authentication.Credentials
import me.ahoo.cosec.api.principal.CoSecPrincipal
import reactor.core.publisher.Mono
import site.weixing.natty.auth.api.AuthAuthenticated
import site.weixing.natty.auth.api.Authenticate
import site.weixing.natty.auth.api.GrantType
import site.weixing.natty.auth.domain.commands.AuthenticatedToken
import site.weixing.natty.auth.domain.commands.IdentityAuthentication
import site.weixing.natty.auth.domain.commands.CredentialsToken

/**
 * Desc
 * @author ambi
 */
//abstract class AbstractPasswordAuthentication :
//    Authentication<UsernamePasswordCredentials, CoSecPrincipal>,
//    IdentityAuthentication {
//
//    override val grantType: GrantType
//        get() = GrantType.PASSWORD
//
//    override val supportCredentials: Class<UsernamePasswordCredentials>
//        get() = UsernamePasswordCredentials::class.java
//
//}
//
//data class UsernamePasswordCredentials(
//    val username: String,
//    val password: String
//) : Credentials, CredentialsToken()