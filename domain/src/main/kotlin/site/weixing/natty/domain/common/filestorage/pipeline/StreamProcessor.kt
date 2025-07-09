package site.weixing.natty.domain.common.filestorage.pipeline

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * 流处理器接口
 * 定义文件流式处理的通用接口，支持零拷贝操作
 */
interface StreamProcessor {
    
    /**
     * 处理器名称
     */
    val name: String
    
    /**
     * 处理器优先级（数值越小优先级越高）
     */
    val priority: Int get() = 100
    
    /**
     * 是否为终端处理器（处理后流终止）
     */
    val isTerminal: Boolean get() = false
    
    /**
     * 处理字节流
     * @param input 输入字节流
     * @param context 处理上下文
     * @return 输出字节流
     */
    fun process(input: Flux<ByteBuffer>, context: ProcessingContext): Flux<ByteBuffer>
    
    /**
     * 检查是否适用于给定上下文
     * @param context 处理上下文
     * @return 是否适用
     */
    fun isApplicable(context: ProcessingContext): Boolean = true
    
    /**
     * 初始化处理器
     * @param context 处理上下文
     * @return 初始化结果
     */
    fun initialize(context: ProcessingContext): Mono<Void> = Mono.empty()
    
    /**
     * 清理处理器资源
     * @param context 处理上下文
     * @return 清理结果
     */
    fun cleanup(context: ProcessingContext): Mono<Void> = Mono.empty()
    
    /**
     * 获取处理器统计信息
     * @return 统计信息
     */
    fun getStatistics(): ProcessorStatistics = ProcessorStatistics.empty(name)
}

/**
 * 处理上下文
 * 包含处理过程中需要的所有信息
 */
data class ProcessingContext(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val uploaderId: String,
    val metadata: MutableMap<String, Any> = mutableMapOf(),
    val processingOptions: ProcessingOptions = ProcessingOptions(),
    val startTime: Long = System.currentTimeMillis()
) {
    
    /**
     * 获取处理持续时间（毫秒）
     */
    fun getProcessingDuration(): Long = System.currentTimeMillis() - startTime
    
    /**
     * 添加元数据
     */
    fun addMetadata(key: String, value: Any): ProcessingContext {
        metadata[key] = value
        return this
    }
    
    /**
     * 获取元数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetadata(key: String): T? = metadata[key] as? T
    
    /**
     * 是否包含指定元数据
     */
    fun hasMetadata(key: String): Boolean = metadata.containsKey(key)
}

/**
 * 处理选项
 */
data class ProcessingOptions(
    val enableVirusScan: Boolean = true,
    val enableCompression: Boolean = false,
    val enableEncryption: Boolean = false,
    val enableThumbnail: Boolean = true,
    val enableChecksumValidation: Boolean = true,
    val compressionLevel: Int = 6, // 1-9
    val encryptionAlgorithm: String = "AES-256-GCM",
    val thumbnailSize: Int = 256,
    val maxProcessingTime: Long = 30_000L, // 30秒
    val bufferSize: Int = 8192
) {
    
    /**
     * 检查是否启用了任何处理功能
     */
    fun hasAnyProcessing(): Boolean {
        return enableVirusScan || enableCompression || enableEncryption || 
               enableThumbnail || enableChecksumValidation
    }
}

/**
 * 处理器统计信息
 */
data class ProcessorStatistics(
    val processorName: String,
    val processedFiles: Long = 0,
    val totalProcessingTime: Long = 0,
    val averageProcessingTime: Double = 0.0,
    val errorCount: Long = 0,
    val bytesProcessed: Long = 0,
    val lastProcessingTime: Long = 0
) {
    companion object {
        fun empty(processorName: String): ProcessorStatistics {
            return ProcessorStatistics(processorName = processorName)
        }
    }
    
    /**
     * 计算平均处理时间
     */
    fun getAverageProcessingTimeMs(): Double {
        return if (processedFiles > 0) totalProcessingTime.toDouble() / processedFiles else 0.0
    }
    
    /**
     * 计算错误率
     */
    fun getErrorRate(): Double {
        return if (processedFiles > 0) errorCount.toDouble() / processedFiles else 0.0
    }
    
    /**
     * 获取吞吐量（MB/s）
     */
    fun getThroughputMBps(): Double {
        return if (totalProcessingTime > 0) {
            (bytesProcessed.toDouble() / (1024 * 1024)) / (totalProcessingTime.toDouble() / 1000)
        } else 0.0
    }
} 