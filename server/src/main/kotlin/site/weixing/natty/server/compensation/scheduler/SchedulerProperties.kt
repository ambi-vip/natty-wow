package site.weixing.natty.server.compensation.scheduler

import me.ahoo.wow.api.naming.EnabledCapable
import site.weixing.natty.server.compensation.configuration.CompensationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties(prefix = SchedulerProperties.PREFIX)
data class SchedulerProperties(
    @DefaultValue("true")
    override var enabled: Boolean = true,
    @DefaultValue("compensation_mutex")
    val mutex: String = "compensation_mutex",
    @DefaultValue("100")
    val batchSize: Int = 100,
    @DefaultValue("PT60S")
    val initialDelay: Duration = Duration.ofSeconds(60),
    @DefaultValue("PT60S")
    val period: Duration = Duration.ofSeconds(60),
) : EnabledCapable {
    companion object {
        const val PREFIX = CompensationProperties.PREFIX + ".scheduler"
    }
}