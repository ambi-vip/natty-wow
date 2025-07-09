package site.weixing.natty.domain.common.filestorage.state

import java.time.LocalDateTime

/**
 * 文件处理状态
 * 跟踪文件处理的各个阶段及元数据
 */
data class FileProcessingState(
    val fileId: String,
    val referenceId: String,
    val status: Status,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val errorMessage: String? = null,
    val processingSteps: List<ProcessingStep> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
) {
    enum class Status {
        INIT,
        VALIDATING,
        PROCESSING,
        SUCCESS,
        FAILED,
        CLEANED
    }
    
    data class ProcessingStep(
        val name: String,
        val startedAt: LocalDateTime,
        val finishedAt: LocalDateTime? = null,
        val success: Boolean = false,
        val error: String? = null
    )
} 