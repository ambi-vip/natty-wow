package site.weixing.natty.domain.common.filestorage.pipeline

import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import java.io.InputStream
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
     * 处理文件上传流
     * @param inputStream 输入流
     * @param storageStrategy 存储策略
     * @param context 处理上下文
     * @return 处理结果
     */
    fun processUpload(
        inputStream: InputStream,
        storageStrategy: FileStorageStrategy,
        context: ProcessingContext
    ): Mono<PipelineResult> {
        
        logger.info("开始流式处理文件: ${context.fileName} (${context.fileSize} bytes)")
        
        return createProcessorChain(context)
            .flatMap { activeProcessors ->
                logger.info("激活的处理器数量: ${activeProcessors.size}, 处理器: ${activeProcessors.map { it.name }}")
                
                // 初始化所有处理器
                initializeProcessors(activeProcessors, context)
                    .then(
                        // 创建输入字节流
                        createInputByteStream(inputStream, context)
                            .let { inputStream ->
                                // 如果没有激活的处理器，直接处理输入流
                                if (activeProcessors.isEmpty()) {
                                    logger.info("没有激活的处理器，直接处理输入流")
                                    inputStream
                                } else {
                                    // 依次通过所有处理器
                                    activeProcessors.fold(inputStream) { stream, processor ->
                                        processWithProcessor(stream, processor, context)
                                    }
                                }
                            }
                            // 收集处理结果
                            .collectList()
                            .map { buffers ->
                                logger.info("收集到 ${buffers.size} 个缓冲区")
                                // 在ByteBuffer被消费之前计算总大小
                                val totalBytes = buffers.sumOf { buffer ->
                                    // 使用duplicate()避免影响原始ByteBuffer的position
                                    buffer.duplicate().remaining().toLong()
                                }
                                logger.info("计算总字节数: $totalBytes")
                                
                                PipelineResult(
                                    processedData = buffers,
                                    context = context,
                                    statistics = collectStatistics(activeProcessors),
                                    success = true,
                                    _totalBytes = totalBytes
                                )
                            }
                    )
                    .publishOn(Schedulers.boundedElastic())
                    .doFinally { 
                        // 清理所有处理器
                        cleanupProcessors(activeProcessors, context).subscribe()
                    }
            }
            .timeout(Duration.ofMillis(context.processingOptions.maxProcessingTime))
            .doOnSuccess { result ->
                logger.info("文件流式处理完成: ${context.fileName} (耗时: ${context.getProcessingDuration()}ms), 总字节数: ${result.getTotalBytes()}")
            }
            .doOnError { error ->
                logger.error("文件流式处理失败: ${context.fileName}", error)
            }
            .onErrorReturn(
                PipelineResult(
                    processedData = emptyList(),
                    context = context,
                    statistics = emptyMap(),
                    success = false,
                    error = "处理失败",
                    _totalBytes = 0L
                )
            )
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
     * 创建输入字节流
     */
    private fun createInputByteStream(
        inputStream: InputStream, 
        context: ProcessingContext
    ): Flux<ByteBuffer> {
        return Flux.generate<ByteBuffer> { sink ->
            try {
                val buffer = ByteArray(context.processingOptions.bufferSize)
                val bytesRead = inputStream.read(buffer)
                
                if (bytesRead == -1) {
                    sink.complete()
                } else {
                    sink.next(ByteBuffer.wrap(buffer, 0, bytesRead))
                }
            } catch (e: Exception) {
                sink.error(e)
            }
        }
        .doFinally { 
            try {
                inputStream.close()
            } catch (e: Exception) {
                logger.warn("关闭输入流失败", e)
            }
        }
    }
    
    /**
     * 使用处理器处理流
     */
    private fun processWithProcessor(
        input: Flux<ByteBuffer>,
        processor: StreamProcessor,
        context: ProcessingContext
    ): Flux<ByteBuffer> {
        return processor.process(input, context)
            .doOnError { error ->
                logger.error("处理器执行失败: ${processor.name}", error)
            }
            .onErrorResume { error ->
                // 处理器失败时，返回原始流
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
    fun toInputStream(): InputStream {
        return toByteArray().inputStream()
    }
} 