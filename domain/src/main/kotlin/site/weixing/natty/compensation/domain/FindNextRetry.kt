package site.weixing.natty.compensation.domain

import me.ahoo.wow.compensation.api.IExecutionFailedState
import reactor.core.publisher.Flux

interface FindNextRetry {
    /**
     * @see IExecutionFailedState.canNextRetry
     */
    fun findNextRetry(limit: Int = 10): Flux<out IExecutionFailedState>
}
