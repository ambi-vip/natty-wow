package site.weixing.natty.compensation.domain

import me.ahoo.wow.compensation.api.IRetrySpec
import me.ahoo.wow.compensation.api.RetryState
import kotlin.math.pow

interface NextRetryAtCalculator {
    fun nextRetryAt(
        minBackoff: Int,
        retries: Int,
        currentRetryAt: Long = System.currentTimeMillis()
    ): Long {
        val multiple = 2.0.pow(retries.toDouble()).toLong()
        val nextRetryDuration = minBackoff * multiple * 1000
        return currentRetryAt + nextRetryDuration
    }

    fun nextRetryState(
        retrySpec: IRetrySpec,
        retries: Int,
        retryAt: Long = System.currentTimeMillis()
    ): RetryState {
        val nextRetryAt = nextRetryAt(retrySpec.minBackoff, retries, retryAt)
        val timeoutAt = retryAt + retrySpec.executionTimeout * 1000
        return RetryState(
            retries = retries,
            retryAt = retryAt,
            timeoutAt = timeoutAt,
            nextRetryAt = nextRetryAt,
        )
    }
}

object DefaultNextRetryAtCalculator : NextRetryAtCalculator
