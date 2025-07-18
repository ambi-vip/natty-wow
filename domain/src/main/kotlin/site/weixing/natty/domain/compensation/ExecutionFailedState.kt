package site.weixing.natty.domain.compensation

import me.ahoo.wow.api.annotation.OnSourcing
import me.ahoo.wow.api.exception.RecoverableType
import me.ahoo.wow.api.messaging.function.FunctionInfoData
import me.ahoo.wow.api.messaging.function.materialize
import me.ahoo.wow.compensation.api.CompensationPrepared
import me.ahoo.wow.compensation.api.ErrorDetails
import me.ahoo.wow.compensation.api.EventId
import me.ahoo.wow.compensation.api.ExecutionFailedApplied
import me.ahoo.wow.compensation.api.ExecutionFailedCreated
import me.ahoo.wow.compensation.api.ExecutionFailedStatus
import me.ahoo.wow.compensation.api.ExecutionSuccessApplied
import me.ahoo.wow.compensation.api.FunctionChanged
import me.ahoo.wow.compensation.api.IExecutionFailedState
import me.ahoo.wow.compensation.api.RecoverableMarked
import me.ahoo.wow.compensation.api.RetrySpec
import me.ahoo.wow.compensation.api.RetrySpec.Companion.materialize
import me.ahoo.wow.compensation.api.RetrySpecApplied
import me.ahoo.wow.compensation.api.RetryState

class ExecutionFailedState(override val id: String) : IExecutionFailedState {
    override lateinit var eventId: EventId
        private set
    override lateinit var function: FunctionInfoData
        private set
    override lateinit var error: ErrorDetails
        private set
    override var executeAt: Long = 0
        private set
    override lateinit var retrySpec: RetrySpec
        private set
    override lateinit var retryState: RetryState
        private set
    override var status: ExecutionFailedStatus = ExecutionFailedStatus.FAILED
        private set
    override var recoverable: RecoverableType = RecoverableType.UNKNOWN
        private set

    @OnSourcing
    fun onCreated(event: ExecutionFailedCreated) {
        this.eventId = event.eventId
        this.function = event.function
        this.error = event.error
        this.executeAt = event.executeAt
        this.retryState = event.retryState
        this.status = ExecutionFailedStatus.FAILED
        this.retrySpec = event.retrySpec
        this.recoverable = event.recoverable
    }

    @Suppress("UnusedParameter")
    @OnSourcing
    fun onPrepared(event: CompensationPrepared) {
        this.retryState = event.retryState
        this.status = ExecutionFailedStatus.PREPARED
    }

    @OnSourcing
    fun onFailed(event: ExecutionFailedApplied) {
        this.executeAt = event.executeAt
        this.status = ExecutionFailedStatus.FAILED
        this.recoverable = event.recoverable
        this.error = event.error
    }

    @OnSourcing
    fun onSuccess(event: ExecutionSuccessApplied) {
        this.executeAt = event.executeAt
        this.status = ExecutionFailedStatus.SUCCEEDED
    }

    @OnSourcing
    fun onRetrySpecApplied(event: RetrySpecApplied) {
        this.retrySpec = event.materialize()
    }

    @OnSourcing
    fun onRecoverableMarked(event: RecoverableMarked) {
        this.recoverable = event.recoverable
    }

    @OnSourcing
    fun onFunctionChanged(event: FunctionChanged) {
        this.function = event.materialize()
    }
}
