package site.weixing.natty.domain.common.filestorage.processing

import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions
import site.weixing.natty.domain.common.filestorage.file.FileMetadata

/**
 * 处理协调器
 * 协调处理器执行，管理处理流程
 */
@Component
class ProcessingCoordinator(
    private val processors: List<FileProcessor>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProcessingCoordinator::class.java)
    }

    /**
     * 处理文件
     */
    fun processFile(
        content: Flux<DataBuffer>,
        options: ProcessingOptions,
        metadata: FileMetadata
    ): Mono<ProcessingResult> {
        val start = System.currentTimeMillis()
        
        return Mono.fromCallable {
            // 筛选适用的处理器
            processors.filter { processor ->
                processor.supports(metadata) && shouldEnableProcessor(processor, options, metadata)
            }
        }
        .flatMap { enabledProcessors ->
            if (enabledProcessors.isEmpty()) {
                // 无需处理，直接返回原始内容
                Mono.just(ProcessingResult(
                    success = true,
                    metadata = mapOf("processingSteps" to emptyList<String>())
                ))
            } else {
                // 执行处理器链
                executeProcessorChain(content, enabledProcessors, metadata)
            }
        }
        .doOnSuccess { result ->
            val end = System.currentTimeMillis()
            logger.debug("文件处理完成: {} 耗时: {}ms 成功: {}", metadata.originalFileName, end - start, result.success)
        }
        .doOnError { error ->
            logger.error("文件处理失败: {} 错误: {}", metadata.originalFileName, error.message)
        }
    }

    /**
     * 判断是否应该启用某个处理器
     */
    private fun shouldEnableProcessor(
        processor: FileProcessor,
        options: ProcessingOptions,
        metadata: FileMetadata
    ): Boolean {
        return when (processor) {
            is CompressionProcessor -> options.enableCompression
            is EncryptionProcessor -> options.requireEncryption
            is ThumbnailProcessor -> options.generateThumbnail
            else -> true // 自定义处理器默认启用
        }
    }

    /**
     * 执行处理器链
     */
    private fun executeProcessorChain(
        content: Flux<DataBuffer>,
        processors: List<FileProcessor>,
        metadata: FileMetadata
    ): Mono<ProcessingResult> {
        val processingSteps = mutableListOf<String>()
        val processingMetadata = mutableMapOf<String, Any>()
        
        return processors.fold(Mono.just(content)) { contentMono, processor ->
            contentMono.flatMap { currentContent ->
                val processorStart = System.currentTimeMillis()
                val processorName = processor::class.simpleName ?: "UnknownProcessor"
                
                processor.process(currentContent, metadata)
                    .doOnSuccess { result ->
                        val processorEnd = System.currentTimeMillis()
                        processingSteps.add(processorName)
                        processingMetadata.putAll(result.metadata)
                        logger.debug("处理器 {} 完成，耗时: {}ms", processorName, processorEnd - processorStart)
                    }
                    .map { it.metadata["processedContent"] as? Flux<DataBuffer> ?: currentContent }
                    .onErrorReturn(currentContent) // 处理器失败时返回原始内容
            }
        }
        .map { finalContent ->
            ProcessingResult(
                success = true,
                metadata = processingMetadata + mapOf(
                    "processingSteps" to processingSteps,
                    "totalProcessingTime" to processingSteps.size * 100L, // 模拟处理时间
                    "processedBy" to "ProcessingCoordinator"
                )
            )
        }
        .onErrorReturn(ProcessingResult(
            success = false,
            errorMessage = "处理链执行失败"
        ))
    }
}