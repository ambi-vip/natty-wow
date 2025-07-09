package site.weixing.natty.domain.common.filestorage.router.rules

import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy

/**
 * 基于文件大小的路由规则
 * 根据文件大小选择最适合的存储策略
 */
class FileSizeBasedRule(
    private val availableStrategies: Map<StorageProvider, FileStorageStrategy>,
    private val configuration: FileSizeRuleConfiguration = FileSizeRuleConfiguration()
) : StorageRoutingRule {

    override fun evaluate(context: FileUploadContext): Mono<RoutingDecision> {
        return Mono.fromCallable {
            val fileSize = context.fileSize
            val preferredProvider = selectProviderBySize(fileSize)
            val strategy = availableStrategies[preferredProvider]
            
            if (strategy != null) {
                RoutingDecision(
                    strategy = strategy,
                    score = calculateScore(fileSize, preferredProvider),
                    reason = "文件大小 ${formatFileSize(fileSize)} 适合使用 ${preferredProvider.displayName}",
                    ruleType = "FILE_SIZE"
                )
            } else {
                // 如果首选策略不可用，回退到LOCAL
                val fallbackStrategy = availableStrategies[StorageProvider.LOCAL]
                    ?: throw IllegalStateException("本地存储策略不可用")
                
                RoutingDecision(
                    strategy = fallbackStrategy,
                    score = 50, // 回退策略分数较低
                    reason = "首选策略不可用，回退到本地存储",
                    ruleType = "FILE_SIZE_FALLBACK"
                )
            }
        }
    }

    override fun getWeight(): Int = 80 // 文件大小是重要的路由因子

    /**
     * 根据文件大小选择存储提供商
     */
    private fun selectProviderBySize(fileSize: Long): StorageProvider {
        return when {
            // 超小文件 (<100KB) - 本地存储最快
            fileSize < configuration.tinyFileThreshold -> StorageProvider.LOCAL
            
            // 小文件 (100KB - 10MB) - 本地存储速度优势
            fileSize < configuration.smallFileThreshold -> StorageProvider.LOCAL
            
            // 中等文件 (10MB - 100MB) - 云存储平衡性能和成本
            fileSize < configuration.mediumFileThreshold -> {
                // 优先选择S3，如果不可用则选择阿里云OSS
                if (availableStrategies.containsKey(StorageProvider.S3)) {
                    StorageProvider.S3
                } else {
                    StorageProvider.ALIYUN_OSS
                }
            }
            
            // 大文件 (>100MB) - 云对象存储成本优势
            else -> {
                // 优先选择阿里云OSS（通常成本更低），如果不可用则选择S3
                if (availableStrategies.containsKey(StorageProvider.ALIYUN_OSS)) {
                    StorageProvider.ALIYUN_OSS
                } else {
                    StorageProvider.S3
                }
            }
        }
    }

    /**
     * 计算路由决策分数
     */
    private fun calculateScore(fileSize: Long, provider: StorageProvider): Int {
        val baseScore = when (provider) {
            StorageProvider.LOCAL -> when {
                fileSize < configuration.smallFileThreshold -> 95 // 小文件本地存储最优
                fileSize < configuration.mediumFileThreshold -> 80 // 中等文件本地存储还行
                else -> 60 // 大文件本地存储不太适合
            }
            StorageProvider.S3 -> when {
                fileSize < configuration.tinyFileThreshold -> 70 // 超小文件云存储过度
                fileSize < configuration.mediumFileThreshold -> 85 // 中等文件S3很好
                else -> 90 // 大文件S3最优
            }
            StorageProvider.ALIYUN_OSS -> when {
                fileSize < configuration.tinyFileThreshold -> 70 // 超小文件云存储过度  
                fileSize < configuration.mediumFileThreshold -> 80 // 中等文件OSS不错
                else -> 95 // 大文件OSS成本最优
            }
        }
        
        return minOf(baseScore, 100)
    }

    /**
     * 格式化文件大小为可读字符串
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
}

/**
 * 文件大小规则配置
 */
data class FileSizeRuleConfiguration(
    val tinyFileThreshold: Long = 100 * 1024L,        // 100KB
    val smallFileThreshold: Long = 10 * 1024 * 1024L, // 10MB  
    val mediumFileThreshold: Long = 100 * 1024 * 1024L // 100MB
) 