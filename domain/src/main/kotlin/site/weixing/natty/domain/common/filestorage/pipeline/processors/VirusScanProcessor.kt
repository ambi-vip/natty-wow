package site.weixing.natty.domain.common.filestorage.pipeline.processors

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.pipeline.StreamProcessor
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessingContext
import site.weixing.natty.domain.common.filestorage.pipeline.ProcessorStatistics
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.springframework.core.io.buffer.DataBuffer

/**
 * 病毒扫描流处理器
 * 模拟病毒扫描功能，检查文件安全性，不修改流内容
 */
class VirusScanProcessor(
    private val configuration: VirusScanConfiguration = VirusScanConfiguration()
) : StreamProcessor {
    
    override val name: String = "VirusScanProcessor"
    override val priority: Int = 5 // 最高优先级，安全第一
    
    private var processedBytes: Long = 0
    private var startTime: Long = 0
    private var scanResult: ScanResult = ScanResult.CLEAN
    
    override fun isApplicable(context: ProcessingContext): Boolean {
        return context.processingOptions.enableVirusScan
    }
    
    override fun initialize(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            processedBytes = 0
            startTime = System.currentTimeMillis()
            scanResult = ScanResult.CLEAN
            
            // 模拟病毒扫描初始化
            if (configuration.logScanActivity) {
                println("病毒扫描器初始化完成：${context.fileName}")
            }
        }
    }
    
    override fun process(input: Flux<DataBuffer>, context: ProcessingContext): Flux<DataBuffer> {
        return input.map { dataBuffer ->
            val bytes = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(bytes)
            processedBytes += bytes.size
            scanForThreats(bytes, context)
            dataBuffer
        }
        .doOnComplete {
            context.addMetadata("virusScanResult", scanResult.name)
            context.addMetadata("virusScanEngine", configuration.engineName)
            context.addMetadata("scanTimeMs", System.currentTimeMillis() - startTime)
            if (configuration.logScanActivity) {
                println("病毒扫描完成：${context.fileName}，结果：${scanResult.displayName}")
            }
        }
    }
    
    override fun cleanup(context: ProcessingContext): Mono<Void> {
        return Mono.fromRunnable {
            // 如果发现威胁，添加警告信息
            if (scanResult != ScanResult.CLEAN) {
                context.addMetadata("securityWarning", "文件可能包含安全威胁")
            }
            
            // 清理扫描器资源
            this.scanResult = ScanResult.CLEAN
        }
    }
    
    override fun getStatistics(): ProcessorStatistics {
        val endTime = System.currentTimeMillis()
        val processingTime = if (startTime > 0) endTime - startTime else 0
        
        return ProcessorStatistics(
            processorName = name,
            processedFiles = if (processedBytes > 0) 1 else 0,
            totalProcessingTime = processingTime,
            averageProcessingTime = processingTime.toDouble(),
            errorCount = if (scanResult == ScanResult.THREAT_DETECTED) 1 else 0,
            bytesProcessed = processedBytes,
            lastProcessingTime = processingTime
        )
    }
    
    /**
     * 模拟病毒扫描逻辑
     */
    private fun scanForThreats(bytes: ByteArray, context: ProcessingContext) {
        // 模拟病毒签名检测
        val content = String(bytes, Charsets.UTF_8)
        
        // 检查已知恶意内容模式（示例）
        val threatPatterns = configuration.threatPatterns
        
        for (pattern in threatPatterns) {
            if (content.contains(pattern, ignoreCase = true)) {
                scanResult = ScanResult.THREAT_DETECTED
                context.addMetadata("threatPattern", pattern)
                return
            }
        }
        
        // 检查文件类型限制
        if (configuration.restrictedContentTypes.contains(context.contentType)) {
            scanResult = ScanResult.SUSPICIOUS
            context.addMetadata("suspiciousReason", "受限制的文件类型")
            return
        }
        
        // 默认扫描结果为清洁
        scanResult = ScanResult.CLEAN
    }
}

/**
 * 病毒扫描配置
 */
data class VirusScanConfiguration(
    val engineName: String = "NattyAV",
    val logScanActivity: Boolean = true,
    val threatPatterns: List<String> = listOf(
        "EICAR-STANDARD-ANTIVIRUS-TEST-FILE", // 标准测试病毒
        "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR", // EICAR测试字符串
        "malware", "virus", "trojan" // 简单的威胁关键词
    ),
    val restrictedContentTypes: Set<String> = setOf(
        "application/x-executable",
        "application/x-msdownload",
        "application/x-dosexec"
    ),
    val maxScanTimeMs: Long = 30_000L
)

/**
 * 扫描结果枚举
 */
enum class ScanResult(val displayName: String) {
    CLEAN("清洁"),
    SUSPICIOUS("可疑"),
    THREAT_DETECTED("发现威胁"),
    SCAN_ERROR("扫描错误")
} 