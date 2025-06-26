package site.weixing.natty.server.compensation.scheduler

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SchedulerProperties::class)
class SchedulerConfiguration
