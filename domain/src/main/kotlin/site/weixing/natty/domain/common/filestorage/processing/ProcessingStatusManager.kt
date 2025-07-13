package site.weixing.natty.domain.common.filestorage.processing

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 处理状态管理器
 * 负责跟踪文件处理的状态和进度
 */
@Service
class ProcessingStatusManager {
    
    // 使用内存存储状态，生产环境建议使用 Redis 或数据库
    private val statusMap = ConcurrentHashMap<String, ProcessingStatus>()
    
    /**
     * 开始处理
     */
    fun startProcessing(
        fileId: String,
        fileName: String,
        processingType: ProcessingType,
        options: ProcessingOptions
    ): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            val status = ProcessingStatus(
                fileId = fileId,
                fileName = fileName,
                processingType = processingType,
                processingOptions = options,
                status = ProcessingStatusEnum.PROCESSING,
                startTime = LocalDateTime.now(),
                progress = 0.0
            )
            statusMap[fileId] = status
            status
        }
    }
    
    /**
     * 更新处理进度
     */
    fun updateProgress(fileId: String, progress: Double, message: String? = null): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            statusMap[fileId]?.let { current ->
                val updated = current.copy(
                    progress = progress.coerceIn(0.0, 100.0),
                    message = message ?: current.message,
                    lastUpdated = LocalDateTime.now()
                )
                statusMap[fileId] = updated
                updated
            } ?: throw IllegalArgumentException("Processing status not found for file: $fileId")
        }
    }
    
    /**
     * 完成处理
     */
    fun completeProcessing(
        fileId: String,
        result: ProcessingResult,
        outputPath: String? = null
    ): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            statusMap[fileId]?.let { current ->
                val updated = current.copy(
                    status = ProcessingStatusEnum.COMPLETED,
                    progress = 100.0,
                    endTime = LocalDateTime.now(),
                    outputPath = outputPath,
                    result = result,
                    message = "处理成功完成"
                )
                statusMap[fileId] = updated
                updated
            } ?: throw IllegalArgumentException("Processing status not found for file: $fileId")
        }
    }
    
    /**
     * 处理失败
     */
    fun failProcessing(
        fileId: String,
        errorMessage: String,
        errorCode: String? = null,
        retryable: Boolean = true
    ): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            statusMap[fileId]?.let { current ->
                val updated = current.copy(
                    status = ProcessingStatusEnum.FAILED,
                    endTime = LocalDateTime.now(),
                    errorMessage = errorMessage,
                    errorCode = errorCode,
                    retryable = retryable,
                    message = "处理失败: $errorMessage"
                )
                statusMap[fileId] = updated
                updated
            } ?: throw IllegalArgumentException("Processing status not found for file: $fileId")
        }
    }
    
    /**
     * 取消处理
     */
    fun cancelProcessing(fileId: String, reason: String = "用户取消"): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            statusMap[fileId]?.let { current ->
                val updated = current.copy(
                    status = ProcessingStatusEnum.CANCELLED,
                    endTime = LocalDateTime.now(),
                    message = "处理已取消: $reason"
                )
                statusMap[fileId] = updated
                updated
            } ?: throw IllegalArgumentException("Processing status not found for file: $fileId")
        }
    }
    
    /**
     * 获取处理状态
     */
    fun getProcessingStatus(fileId: String): Mono<ProcessingStatus> {
        return statusMap[fileId]?.toMono() 
            ?: Mono.error(IllegalArgumentException("Processing status not found for file: $fileId"))
    }
    
    /**
     * 获取所有处理状态
     */
    fun getAllProcessingStatus(): Mono<List<ProcessingStatus>> {
        return Mono.fromCallable {
            statusMap.values.toList()
        }
    }
    
    /**
     * 获取指定状态的处理记录
     */
    fun getProcessingStatusByStatus(status: ProcessingStatusEnum): Mono<List<ProcessingStatus>> {
        return Mono.fromCallable {
            statusMap.values.filter { it.status == status }
        }
    }
    
    /**
     * 清理已完成的处理状态（避免内存泄漏）
     */
    fun cleanupCompletedStatus(beforeTime: LocalDateTime): Mono<Int> {
        return Mono.fromCallable {
            val toRemove = statusMap.values.filter { status ->
                (status.status == ProcessingStatusEnum.COMPLETED || 
                 status.status == ProcessingStatusEnum.FAILED ||
                 status.status == ProcessingStatusEnum.CANCELLED) &&
                status.endTime?.isBefore(beforeTime) == true
            }
            
            toRemove.forEach { status ->
                statusMap.remove(status.fileId)
            }
            
            toRemove.size
        }
    }
    
    /**
     * 重置处理状态（用于重试）
     */
    fun resetProcessingStatus(fileId: String): Mono<ProcessingStatus> {
        return Mono.fromCallable {
            statusMap[fileId]?.let { current ->
                if (current.retryable) {
                    val reset = current.copy(
                        status = ProcessingStatusEnum.PENDING,
                        progress = 0.0,
                        endTime = null,
                        errorMessage = null,
                        errorCode = null,
                        result = null,
                        outputPath = null,
                        message = "等待重试",
                        retryCount = current.retryCount + 1,
                        lastUpdated = LocalDateTime.now()
                    )
                    statusMap[fileId] = reset
                    reset
                } else {
                    throw IllegalStateException("Processing status for file $fileId is not retryable")
                }
            } ?: throw IllegalArgumentException("Processing status not found for file: $fileId")
        }
    }
}

/**
 * 处理状态枚举
 */
enum class ProcessingStatusEnum {
    PENDING,     // 等待处理
    PROCESSING,  // 处理中
    COMPLETED,   // 完成
    FAILED,      // 失败
    CANCELLED    // 已取消
}

/**
 * 处理状态数据类
 */
data class ProcessingStatus(
    val fileId: String,
    val fileName: String,
    val processingType: ProcessingType,
    val processingOptions: ProcessingOptions,
    val status: ProcessingStatusEnum,
    val progress: Double = 0.0,
    val message: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    val outputPath: String? = null,
    val result: ProcessingResult? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val retryable: Boolean = true,
    val retryCount: Int = 0
) {
    /**
     * 计算处理耗时
     */
    fun getDurationMs(): Long? {
        return endTime?.let { end ->
            java.time.Duration.between(startTime, end).toMillis()
        }
    }
    
    /**
     * 是否处理中
     */
    fun isProcessing(): Boolean {
        return status == ProcessingStatusEnum.PROCESSING || status == ProcessingStatusEnum.PENDING
    }
    
    /**
     * 是否已完成（成功或失败）
     */
    fun isCompleted(): Boolean {
        return status == ProcessingStatusEnum.COMPLETED || 
               status == ProcessingStatusEnum.FAILED || 
               status == ProcessingStatusEnum.CANCELLED
    }
}

/**
 * 处理类型枚举（与 API 层保持一致）
 */
enum class ProcessingType {
    ENCRYPTION,      // 加密处理
    COMPRESSION,     // 压缩处理
    THUMBNAIL,       // 缩略图生成
    VIRUS_SCAN,      // 病毒扫描
    OCR,            // 文字识别
    WATERMARK,      // 水印添加
    CUSTOM          // 自定义处理
}

/**
 * 处理结果数据类（与 API 层保持一致）
 */
data class ProcessingResult(
    val success: Boolean,
    val outputPath: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val artifacts: List<ProcessingArtifact> = emptyList(),
    val metrics: ProcessingMetrics? = null,
    val errorMessage: String? = null
)

/**
 * 处理产物
 */
data class ProcessingArtifact(
    val type: String,
    val path: String,
    val size: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 处理指标
 */
data class ProcessingMetrics(
    val processingTimeMs: Long,
    val inputSize: Long,
    val outputSize: Long,
    val compressionRatio: Double? = null,
    val qualityScore: Double? = null
)