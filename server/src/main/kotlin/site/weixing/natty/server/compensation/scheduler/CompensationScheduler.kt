package site.weixing.natty.server.compensation.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.simba.core.MutexContendServiceFactory
import me.ahoo.simba.schedule.AbstractScheduler
import me.ahoo.simba.schedule.ScheduleConfig
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.toCommandMessage
import me.ahoo.wow.compensation.api.PrepareCompensation
import site.weixing.natty.compensation.domain.FindNextRetry
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@ConditionalOnSchedulerEnabled
class CompensationScheduler(
    private val findNextRetry: FindNextRetry,
    private val commandGateway: CommandGateway,
    private val schedulerProperties: SchedulerProperties,
    contendServiceFactory: MutexContendServiceFactory
) :
    AbstractScheduler(
        mutex = schedulerProperties.mutex,
        contendServiceFactory = contendServiceFactory
    ),
    SmartLifecycle {
    companion object {
        private val log = KotlinLogging.logger { }
        const val WORKER_NAME = "CompensationScheduler"
    }

    fun retry(limit: Int = 100): Mono<Long> {
        return findNextRetry.findNextRetry(limit)
            .flatMap {
                log.debug {
                    "retry - ExecutionFailed[${it.id}] - ${it.retryState} - ${it.eventId} - ${it.function}"
                }
                val commandMessage = PrepareCompensation(it.id).toCommandMessage()
                commandGateway.send(commandMessage).thenReturn(commandMessage)
            }
            .count()
    }

    override val config: ScheduleConfig =
        ScheduleConfig.delay(schedulerProperties.initialDelay, schedulerProperties.period)
    override val worker: String
        get() = WORKER_NAME

    override fun work() {
        log.info {
            "Start retry - batchSize:[${schedulerProperties.batchSize}]."
        }
        val count = retry(schedulerProperties.batchSize)
            .block()
        log.info {
            "Complete retry - batchSize:[${schedulerProperties.batchSize}] - count:[${count}]."
        }
    }

    override fun isRunning(): Boolean {
        return super.running
    }
}