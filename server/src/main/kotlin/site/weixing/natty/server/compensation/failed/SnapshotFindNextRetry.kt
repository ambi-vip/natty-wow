package site.weixing.natty.server.compensation.failed

import me.ahoo.wow.api.exception.RecoverableType
import me.ahoo.wow.compensation.api.ExecutionFailedStatus
import me.ahoo.wow.compensation.api.IExecutionFailedState
import site.weixing.natty.compensation.domain.ExecutionFailedState
import site.weixing.natty.compensation.domain.FindNextRetry
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.dsl.listQuery
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import me.ahoo.wow.serialization.MessageRecords
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import site.weixing.natty.compensation.domain.ExecutionFailedStateProperties.IS_RETRYABLE
import site.weixing.natty.compensation.domain.ExecutionFailedStateProperties.RECOVERABLE
import site.weixing.natty.compensation.domain.ExecutionFailedStateProperties.STATUS
import site.weixing.natty.compensation.domain.ExecutionFailedStateProperties.RETRY_STATE__NEXT_RETRY_AT
import site.weixing.natty.compensation.domain.ExecutionFailedStateProperties.RETRY_STATE__TIMEOUT_AT

@Primary
@Repository
class SnapshotFindNextRetry(
    private val queryService: SnapshotQueryService<ExecutionFailedState>
) : FindNextRetry {

    override fun findNextRetry(limit: Int): Flux<out IExecutionFailedState> {
        val currentTime = System.currentTimeMillis()
        return listQuery {
            limit(limit)
            condition {
                nestedState()
                RECOVERABLE ne RecoverableType.UNRECOVERABLE.name
                IS_RETRYABLE eq true
                RETRY_STATE__NEXT_RETRY_AT lte currentTime
                or {
                    STATUS eq ExecutionFailedStatus.FAILED.name
                    and {
                        STATUS eq ExecutionFailedStatus.PREPARED.name
                        RETRY_STATE__TIMEOUT_AT lte currentTime
                    }
                }
            }
            sort {
                MessageRecords.VERSION.asc()
            }
        }.query(queryService)
            .toState()
    }

}