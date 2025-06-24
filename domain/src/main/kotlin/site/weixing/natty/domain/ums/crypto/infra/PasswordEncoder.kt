package site.weixing.natty.domain.ums.crypto.infra

/**
 * PasswordEncoder
 * @author ambi
 */
interface PasswordEncoder {

    fun encode(rawPassword: CharSequence?): String

    fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean

    fun upgradeEncoding(encodedPassword: String?): Boolean {
        return false
    }

}