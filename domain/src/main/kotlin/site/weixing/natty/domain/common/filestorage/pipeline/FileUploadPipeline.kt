package site.weixing.natty.domain.common.filestorage.pipeline

import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import java.nio.ByteBuffer
import java.time.Duration

/**
 * 文件上传流式处理管道
 * 协调多个流处理器，实现零拷贝文件处理
 */
class FileUploadPipeline(
    private val processors: List<StreamProcessor>,
    private val configuration: PipelineConfiguration = PipelineConfiguration()
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(FileUploadPipeline::class.java)
    }
    
    /**
     * 处理文件上传流（全链路 DataBuffer 流式处理）
     * @param dataBufferFlux 上传文件的 DataBuffer 流
     * @param context 处理上下文
     * @return 处理后的 DataBuffer 流
     */
    fun processUpload(
        dataBufferFlux: Flux<DataBuffer>,
        context: ProcessingContext
    ): Flux<DataBuffer> {
        logger.info("开始流式处理文件: ${context.fileName} (${context.fileSize} bytes)")
        return createProcessorChain(context)
            .flatMapMany { activeProcessors ->
                logger.info("激活的处理器数量: ${activeProcessors.size}, 处理器: ${activeProcessors.map { it.name }}")
                // 初始化所有处理器
                initializeProcessors(activeProcessors, context)
                    .thenMany(
                        if (activeProcessors.isEmpty()) {
                            logger.info("没有激活的处理器，直接处理输入流")
                            dataBufferFlux
                        } else {
                            // 依次通过所有处理器
                            activeProcessors.fold(dataBufferFlux) { stream, processor ->
                                processWithProcessor(stream, processor, context)
                            }
                        }
                    )
                    .publishOn(Schedulers.boundedElastic())
                    .doFinally {
                        cleanupProcessors(activeProcessors, context).subscribe()
                    }
            }
            .doOnComplete {
                logger.debug("文件流式处理完成: ${context.fileName}")
            }
            .doOnError { error ->
                logger.error("文件流式处理失败: ${context.fileName}", error)
            }
    }
    
    /**
     * 创建处理器链
     */
    private fun createProcessorChain(context: ProcessingContext): Mono<List<StreamProcessor>> {
        return Mono.fromCallable {
            processors
                .filter { it.isApplicable(context) }
                .sortedBy { it.priority }
                .also { activeProcessors ->
                    logger.debug("激活处理器: ${activeProcessors.map { it.name }}")
                }
        }
    }
    
    /**
     * 初始化处理器
     */
    private fun initializeProcessors(
        processors: List<StreamProcessor>, 
        context: ProcessingContext
    ): Mono<Void> {
        return Flux.fromIterable(processors)
            .flatMap { processor ->
                processor.initialize(context)
                    .doOnError { error ->
                        logger.warn("处理器初始化失败: ${processor.name}", error)
                    }
                    .onErrorResume { Mono.empty() }
            }
            .then()
    }
    
    /**
     * 清理处理器
     */
    private fun cleanupProcessors(
        processors: List<StreamProcessor>, 
        context: ProcessingContext
    ): Mono<Void> {
        return Flux.fromIterable(processors)
            .flatMap { processor ->
                processor.cleanup(context)
                    .doOnError { error ->
                        logger.warn("处理器清理失败: ${processor.name}", error)
                    }
                    .onErrorResume { Mono.empty() }
            }
            .then()
    }
    
    /**
     * 处理器链路全部为 Flux<DataBuffer>
     */
    private fun processWithProcessor(
        input: Flux<DataBuffer>,
        processor: StreamProcessor,
        context: ProcessingContext
    ): Flux<DataBuffer> {
        return processor.process(input, context)
            .doOnError { error ->
                logger.error("处理器执行失败: ${processor.name}", error)
            }
            .onErrorResume { error ->
                logger.warn("处理器 ${processor.name} 失败，跳过处理: ${error.message}")
                input
            }
    }
    
    /**
     * 收集统计信息
     */
    private fun collectStatistics(processors: List<StreamProcessor>): Map<String, ProcessorStatistics> {
        return processors.associate { processor ->
            processor.name to processor.getStatistics()
        }
    }
}

/**
 * 管道配置
 */
data class PipelineConfiguration(
    val enableParallelProcessing: Boolean = false,
    val maxConcurrentProcessors: Int = 4,
    val defaultTimeout: Duration = Duration.ofSeconds(30),
    val enableStatistics: Boolean = true,
    val enableErrorRecovery: Boolean = true,
    val bufferSize: Int = 8192
)

/**
 * 管道处理结果
 */
data class PipelineResult(
    val processedData: List<ByteBuffer>,
    val context: ProcessingContext,
    val statistics: Map<String, ProcessorStatistics>,
    val success: Boolean,
    val error: String? = null,
    val processingTime: Long = System.currentTimeMillis(),
    private val _totalBytes: Long? = null
) {
    
    // 缓存的总字节数，避免重复计算和ByteBuffer状态问题
    private val cachedTotalBytes: Long by lazy {
        _totalBytes ?: processedData.sumOf { buffer ->
            // 使用duplicate()避免影响原始ByteBuffer的position
            buffer.duplicate().remaining().toLong()
        }
    }
    
    /**
     * 获取处理后的总字节数
     */
    fun getTotalBytes(): Long = cachedTotalBytes
    
    /**
     * 获取处理时间
     */
    fun getProcessingDurationMs(): Long {
        return context.getProcessingDuration()
    }
    
    /**
     * 转换为字节数组
     */
    fun toByteArray(): ByteArray {
        val totalSize = getTotalBytes().toInt()
        val result = ByteArray(totalSize)
        var offset = 0
        
        processedData.forEach { buffer ->
            // 使用duplicate()避免修改原始ByteBuffer的position
            val duplicateBuffer = buffer.duplicate()
            val remaining = duplicateBuffer.remaining()
            duplicateBuffer.get(result, offset, remaining)
            offset += remaining
        }
        
        return result
    }
    
    /**
     * 转换为输入流
     */
    fun toInputStream(): java.io.InputStream {
        return toByteArray().inputStream()
    }
} 