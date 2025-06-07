package site.weixing.natty.server.ums.auth

import site.weixing.natty.auth.domain.commands.IdentityAuthentication

/**
 * Desc
 * @author ambi
 */
interface UserIdentityAuthentication : IdentityAuthentication {

    override val accountType: String
        get() = "USER"

}