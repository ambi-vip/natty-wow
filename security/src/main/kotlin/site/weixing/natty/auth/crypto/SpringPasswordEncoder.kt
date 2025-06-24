package site.weixing.natty.auth.crypto

import org.springframework.stereotype.Component
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder

/**
 * SpringPasswordEncoder
 * @author ambi
 */
@Component
class SpringPasswordEncoder(
    private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder
) : PasswordEncoder {

    override fun encode(rawPassword: CharSequence?): String {
        return passwordEncoder.encode(rawPassword)
    }

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}