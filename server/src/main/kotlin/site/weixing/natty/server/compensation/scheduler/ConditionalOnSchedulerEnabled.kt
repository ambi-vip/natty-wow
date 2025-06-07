package site.weixing.natty.server.compensation.scheduler

import me.ahoo.wow.spring.boot.starter.ENABLED_SUFFIX_KEY
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

const val ENABLED_KEY: String = SchedulerProperties.PREFIX + ENABLED_SUFFIX_KEY

@ConditionalOnProperty(
    ENABLED_KEY,
    havingValue = "true",
    matchIfMissing = true,
)
annotation class ConditionalOnSchedulerEnabled
