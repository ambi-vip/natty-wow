package site.weixing.natty.server.common.filestorage.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.processing.ProcessingStatusManager
import site.weixing.natty.domain.common.filestorage.processing.ProcessingStatus
import site.weixing.natty.domain.common.filestorage.processing.ProcessingStatusEnum
import java.time.LocalDateTime

/**
 * 文件处理状态控制器
 * 提供处理状态查询和管理接口
 */
@RestController
@RequestMapping("/files/processing")
class FileProcessingStatusController(
    private val processingStatusManager: ProcessingStatusManager
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 获取文件处理状态
     */
    @GetMapping("/status/{fileId}")
    fun getProcessingStatus(@PathVariable fileId: String): Mono<ResponseEntity<ProcessingStatusResponse>> {
        logger.debug { "查询文件处理状态: $fileId" }
        
        return processingStatusManager.getProcessingStatus(fileId)
            .map { status ->
                ResponseEntity.ok(ProcessingStatusResponse.from(status))
            }
            .onErrorReturn(
                ResponseEntity.notFound().build()
            )
    }
    
    /**
     * 获取所有处理状态
     */
    @GetMapping("/status")
    fun getAllProcessingStatus(): Mono<ResponseEntity<List<ProcessingStatusResponse>>> {
        logger.debug { "查询所有文件处理状态" }
        
        return processingStatusManager.getAllProcessingStatus()
            .map { statuses ->
                ResponseEntity.ok(statuses.map { ProcessingStatusResponse.from(it) })
            }
    }
    
    /**
     * 根据状态查询处理记录
     */
    @GetMapping("/status/by-status/{status}")
    fun getProcessingStatusByStatus(
        @PathVariable status: ProcessingStatusEnum
    ): Mono<ResponseEntity<List<ProcessingStatusResponse>>> {
        logger.debug { "查询指定状态的处理记录: $status" }
        
        return processingStatusManager.getProcessingStatusByStatus(status)
            .map { statuses ->
                ResponseEntity.ok(statuses.map { ProcessingStatusResponse.from(it) })
            }
    }
    
    /**
     * 取消文件处理
     */
    @PostMapping("/cancel/{fileId}")
    fun cancelProcessing(
        @PathVariable fileId: String,
        @RequestParam(value = "reason", required = false) reason: String?
    ): Mono<ResponseEntity<ProcessingStatusResponse>> {
        logger.info { "取消文件处理: $fileId, 原因: ${reason ?: "用户取消"}" }
        
        return processingStatusManager.cancelProcessing(fileId, reason ?: "用户取消")
            .map { status ->
                ResponseEntity.ok(ProcessingStatusResponse.from(status))
            }
            .onErrorReturn(
                ResponseEntity.badRequest().build()
            )
    }
    
    /**
     * 重试文件处理
     */
    @PostMapping("/retry/{fileId}")
    fun retryProcessing(@PathVariable fileId: String): Mono<ResponseEntity<ProcessingStatusResponse>> {
        logger.info { "重试文件处理: $fileId" }
        
        return processingStatusManager.resetProcessingStatus(fileId)
            .map { status ->
                ResponseEntity.ok(ProcessingStatusResponse.from(status))
            }
            .onErrorReturn(
                ResponseEntity.badRequest().build()
            )
    }
    
    /**
     * 清理已完成的处理状态
     */
    @PostMapping("/cleanup")
    fun cleanupCompletedStatus(
        @RequestParam(value = "beforeHours", required = false) beforeHours: Int?
    ): Mono<ResponseEntity<Map<String, Any>>> {
        val hours = beforeHours ?: 24 // 默认清理24小时前的记录
        val beforeTime = LocalDateTime.now().minusHours(hours.toLong())
        
        logger.info { "清理${hours}小时前已完成的处理状态" }
        
        return processingStatusManager.cleanupCompletedStatus(beforeTime)
            .map { cleanedCount ->
                ResponseEntity.ok(
                    mapOf(
                        "message" to "清理完成",
                        "cleanedCount" to cleanedCount,
                        "beforeTime" to beforeTime.toString(),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
    }
    
    /**
     * 获取处理统计信息
     */
    @GetMapping("/stats")
    fun getProcessingStats(): Mono<ResponseEntity<ProcessingStatsResponse>> {
        logger.debug { "查询处理统计信息" }
        
        return processingStatusManager.getAllProcessingStatus()
            .map { statuses ->
                val stats = ProcessingStatsResponse(
                    totalCount = statuses.size,
                    processingCount = statuses.count { it.status == ProcessingStatusEnum.PROCESSING },
                    pendingCount = statuses.count { it.status == ProcessingStatusEnum.PENDING },
                    completedCount = statuses.count { it.status == ProcessingStatusEnum.COMPLETED },
                    failedCount = statuses.count { it.status == ProcessingStatusEnum.FAILED },
                    cancelledCount = statuses.count { it.status == ProcessingStatusEnum.CANCELLED },
                    averageProcessingTimeMs = statuses
                        .filter { it.isCompleted() }
                        .mapNotNull { it.getDurationMs() }
                        .takeIf { it.isNotEmpty() }
                        ?.average()?.toLong()
                )
                ResponseEntity.ok(stats)
            }
    }
}

/**
 * 处理状态响应
 */
data class ProcessingStatusResponse(
    val fileId: String,
    val fileName: String,
    val processingType: String,
    val status: String,
    val progress: Double,
    val message: String,
    val startTime: String,
    val endTime: String?,
    val lastUpdated: String,
    val outputPath: String?,
    val errorMessage: String?,
    val errorCode: String?,
    val retryable: Boolean,
    val retryCount: Int,
    val durationMs: Long?,
    val processingOptions: Map<String, Any>
) {
    companion object {
        fun from(status: ProcessingStatus): ProcessingStatusResponse {
            return ProcessingStatusResponse(
                fileId = status.fileId,
                fileName = status.fileName,
                processingType = status.processingType.name,
                status = status.status.name,
                progress = status.progress,
                message = status.message,
                startTime = status.startTime.toString(),
                endTime = status.endTime?.toString(),
                lastUpdated = status.lastUpdated.toString(),
                outputPath = status.outputPath,
                errorMessage = status.errorMessage,
                errorCode = status.errorCode,
                retryable = status.retryable,
                retryCount = status.retryCount,
                durationMs = status.getDurationMs(),
                processingOptions = mapOf(
                    "requireEncryption" to status.processingOptions.requireEncryption,
                    "enableCompression" to status.processingOptions.enableCompression,
                    "generateThumbnail" to status.processingOptions.generateThumbnail,
                    "customProcessors" to status.processingOptions.customProcessors,
                )
            )
        }
    }
}

/**
 * 处理统计响应
 */
data class ProcessingStatsResponse(
    val totalCount: Int,
    val processingCount: Int,
    val pendingCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val cancelledCount: Int,
    val averageProcessingTimeMs: Long?
)