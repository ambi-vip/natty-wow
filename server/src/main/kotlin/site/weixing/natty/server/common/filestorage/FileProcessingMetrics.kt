package site.weixing.natty.server.common.filestorage

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * 文件处理性能指标收集
 */
@Component
class FileProcessingMetrics(
    private val meterRegistry: MeterRegistry
) {
    fun recordProcessingTime(durationMs: Long) {
        meterRegistry.timer("file.processing.time").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    fun incrementSuccess() {
        meterRegistry.counter("file.processing.success").increment()
    }
    fun incrementFailure() {
        meterRegistry.counter("file.processing.failure").increment()
    }
    fun recordFileSize(size: Long) {
        meterRegistry.summary("file.processing.size").record(size.toDouble())
    }
} 