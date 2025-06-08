package site.weixing.natty.server.compensation.configuration

import site.weixing.natty.domain.compensation.DefaultNextRetryAtCalculator
import site.weixing.natty.domain.compensation.NextRetryAtCalculator
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CompensationProperties::class)
class CompensationConfiguration {

    @Bean
    fun nextRetryAtCalculator(): NextRetryAtCalculator {
        return DefaultNextRetryAtCalculator
    }

    @Bean
    fun corsFilter(): CorsWebFilter {
        val config = CorsConfiguration().applyPermitDefaultValues()
        config.allowedMethods = listOf("*")
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
        return CorsWebFilter(source)
    }
}