package site.weixing.natty.auth

import me.ahoo.cosec.authentication.CompositeAuthentication
import me.ahoo.cosec.token.TokenConverter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import site.weixing.natty.auth.authorization.DefaultAuthenticationManager
import site.weixing.natty.domain.auth.AuthHandler
import site.weixing.natty.domain.auth.commands.AuthenticationManager

@Configuration
open class CoSecConfiguration {

    private val logger = LoggerFactory.getLogger(CoSecConfiguration::class.java)

    @Bean(name = ["defaultAuthenticationManager"])
//    @ConditionalOnBean(CompositeAuthentication::class)
    open fun authenticationManager(
        compositeAuthentication: CompositeAuthentication,
        tokenConverter: TokenConverter
    ): AuthenticationManager {
        return DefaultAuthenticationManager(compositeAuthentication, tokenConverter)
    }

    @Bean
    @ConditionalOnBean(AuthenticationManager::class)
    open fun defaultAuthHandler(
        authenticationManager: AuthenticationManager,
    ) : AuthHandler {
        return DefaultAuthHandler(authenticationManager);
    }


    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(8)
    }

}