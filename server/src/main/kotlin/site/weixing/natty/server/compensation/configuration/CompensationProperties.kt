package site.weixing.natty.server.compensation.configuration

import me.ahoo.wow.api.annotation.Retry
import me.ahoo.wow.compensation.api.IRetrySpec
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = CompensationProperties.PREFIX)
data class CompensationProperties(
    val host: String = "",
    @DefaultValue("10")
    override val maxRetries: Int = Retry.DEFAULT_MAX_RETRIES,
    @DefaultValue("180")
    override val minBackoff: Int = Retry.DEFAULT_MIN_BACKOFF,
    @DefaultValue("120")
    override val executionTimeout: Int = Retry.DEFAULT_EXECUTION_TIMEOUT,
) : IRetrySpec {
    companion object {
        const val PREFIX = me.ahoo.wow.spring.boot.starter.compensation.CompensationProperties.PREFIX
    }

}
