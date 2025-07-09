package site.weixing.natty.domain.common.filestorage.router.rules

import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.router.AccessPattern
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy

/**
 * 基于访问模式的路由规则
 * 根据预期的文件访问频率选择最适合的存储策略
 */
class AccessPatternBasedRule(
    private val availableStrategies: Map<StorageProvider, FileStorageStrategy>,
    private val configuration: AccessPatternRuleConfiguration = AccessPatternRuleConfiguration()
) : StorageRoutingRule {

    override fun evaluate(context: FileUploadContext): Mono<RoutingDecision> {
        return Mono.fromCallable {
            val accessPattern = context.expectedAccessPattern
            val preferredProvider = selectProviderByAccessPattern(accessPattern, context)
            val strategy = availableStrategies[preferredProvider]
            
            if (strategy != null) {
                RoutingDecision(
                    strategy = strategy,
                    score = calculateScore(accessPattern, preferredProvider, context),
                    reason = buildDecisionReason(accessPattern, preferredProvider, context),
                    ruleType = "ACCESS_PATTERN",
                    metadata = mapOf(
                        "accessPattern" to accessPattern.name,
                        "isPublic" to context.isPublic,
                        "priorityLevel" to context.priorityLevel.name,
                        "accessScore" to context.getEstimatedAccessScore()
                    )
                )
            } else {
                // 回退策略
                val fallbackStrategy = availableStrategies[StorageProvider.LOCAL]
                    ?: throw IllegalStateException("本地存储策略不可用")
                
                RoutingDecision(
                    strategy = fallbackStrategy,
                    score = 60, // 回退策略分数
                    reason = "首选存储策略不可用，回退到本地存储",
                    ruleType = "ACCESS_PATTERN_FALLBACK"
                )
            }
        }
    }

    override fun getWeight(): Int = 65 // 访问模式是重要的路由因子

    /**
     * 根据访问模式选择存储提供商
     */
    private fun selectProviderByAccessPattern(pattern: AccessPattern, context: FileUploadContext): StorageProvider {
        return when (pattern) {
            AccessPattern.HOT -> {
                // 热数据需要最快访问速度
                when {
                    // 公开的热数据优先云存储+CDN
                    context.isPublic -> StorageProvider.S3
                    
                    // 私有的热数据优先本地存储
                    !context.isPublic -> StorageProvider.LOCAL
                    
                    // 小文件热数据本地存储最快
                    context.isSmallFile() -> StorageProvider.LOCAL
                    
                    else -> StorageProvider.S3
                }
            }
            
            AccessPattern.WARM -> {
                // 温数据平衡性能和成本
                when {
                    // 大文件温数据适合云存储
                    context.isLargeFile() -> {
                        if (availableStrategies.containsKey(StorageProvider.S3)) {
                            StorageProvider.S3
                        } else {
                            StorageProvider.ALIYUN_OSS
                        }
                    }
                    
                    // 小文件温数据本地存储更快
                    context.isSmallFile() -> StorageProvider.LOCAL
                    
                    // 公开文件考虑CDN
                    context.isPublic -> StorageProvider.S3
                    
                    else -> StorageProvider.LOCAL
                }
            }
            
            AccessPattern.COLD -> {
                // 冷数据优化存储成本
                when {
                    // 冷数据优先成本最低的云存储
                    availableStrategies.containsKey(StorageProvider.ALIYUN_OSS) -> StorageProvider.ALIYUN_OSS
                    
                    // 其次是S3
                    availableStrategies.containsKey(StorageProvider.S3) -> StorageProvider.S3
                    
                    // 最后是本地存储
                    else -> StorageProvider.LOCAL
                }
            }
        }
    }

    /**
     * 计算路由决策分数
     */
    private fun calculateScore(pattern: AccessPattern, provider: StorageProvider, context: FileUploadContext): Int {
        var baseScore = when (pattern) {
            AccessPattern.HOT -> {
                when (provider) {
                    StorageProvider.LOCAL -> {
                        if (context.isSmallFile()) 95 // 小热文件本地最优
                        else if (context.isPublic) 70 // 大热文件本地不够好（无CDN）
                        else 85 // 私有大热文件本地还可以
                    }
                    StorageProvider.S3 -> {
                        if (context.isPublic) 90 // 公开热文件S3+CDN最优
                        else 80 // 私有热文件S3也不错
                    }
                    StorageProvider.ALIYUN_OSS -> {
                        if (context.isPublic) 85 // 公开热文件OSS+CDN不错
                        else 75 // 私有热文件OSS还行
                    }
                }
            }
            
            AccessPattern.WARM -> {
                when (provider) {
                    StorageProvider.LOCAL -> {
                        if (context.isSmallFile()) 90 // 小温文件本地很好
                        else if (context.isLargeFile()) 70 // 大温文件本地一般
                        else 85 // 中等温文件本地不错
                    }
                    StorageProvider.S3 -> {
                        if (context.isLargeFile()) 90 // 大温文件S3很好
                        else 80 // 其他温文件S3也不错
                    }
                    StorageProvider.ALIYUN_OSS -> {
                        if (context.isLargeFile()) 85 // 大温文件OSS不错
                        else 75 // 其他温文件OSS还行
                    }
                }
            }
            
            AccessPattern.COLD -> {
                when (provider) {
                    StorageProvider.LOCAL -> {
                        if (context.isSmallFile()) 80 // 小冷文件本地还行
                        else 60 // 大冷文件本地浪费空间
                    }
                    StorageProvider.S3 -> 85 // 冷文件S3归档很好
                    StorageProvider.ALIYUN_OSS -> 95 // 冷文件OSS成本最优
                }
            }
        }
        
        // 根据优先级调整分数
        baseScore += when (context.priorityLevel.level) {
            4 -> 10 // 紧急优先级
            3 -> 5  // 高优先级
            1 -> -5 // 低优先级
            else -> 0 // 正常优先级
        }
        
        // 根据公开性调整分数
        if (context.isPublic && provider == StorageProvider.S3) {
            baseScore += 5 // 公开文件S3有CDN优势
        }
        
        // 根据地理位置调整分数（如果有）
        context.geoLocation?.let { location ->
            when {
                location.contains("CN", ignoreCase = true) && provider == StorageProvider.ALIYUN_OSS -> {
                    baseScore += 10 // 中国地区OSS更优
                }
                !location.contains("CN", ignoreCase = true) && provider == StorageProvider.S3 -> {
                    baseScore += 10 // 海外地区S3更优
                }
            }
        }
        
        return maxOf(minOf(baseScore, 100), 0)
    }

    /**
     * 构建决策原因说明
     */
    private fun buildDecisionReason(pattern: AccessPattern, provider: StorageProvider, context: FileUploadContext): String {
        val patternDesc = when (pattern) {
            AccessPattern.HOT -> "频繁访问"
            AccessPattern.WARM -> "正常访问"
            AccessPattern.COLD -> "低频访问"
        }
        
        val providerAdvantage = when (pattern to provider) {
            AccessPattern.HOT to StorageProvider.LOCAL -> "本地存储提供最快访问速度"
            AccessPattern.HOT to StorageProvider.S3 -> "S3配合CDN提供全球加速"
            AccessPattern.WARM to StorageProvider.S3 -> "S3平衡性能和成本"
            AccessPattern.WARM to StorageProvider.LOCAL -> "本地存储提供快速访问"
            AccessPattern.COLD to StorageProvider.ALIYUN_OSS -> "阿里云OSS提供最低存储成本"
            AccessPattern.COLD to StorageProvider.S3 -> "S3归档存储成本优化"
            else -> "${provider.displayName}适合此访问模式"
        }
        
        val fileTypeContext = when {
            context.isPublic -> "公开文件"
            context.isSmallFile() -> "小文件"
            context.isLargeFile() -> "大文件"
            else -> "文件"
        }
        
        return "$fileTypeContext 预期$patternDesc，$providerAdvantage"
    }

    override fun isApplicable(context: FileUploadContext): Boolean {
        // 只有当明确指定了访问模式时才应用此规则
        return context.expectedAccessPattern != AccessPattern.WARM || 
               context.priorityLevel.level > 2 ||
               context.isPublic
    }
}

/**
 * 访问模式规则配置
 */
data class AccessPatternRuleConfiguration(
    val enableGeoOptimization: Boolean = true,
    val enableCdnOptimization: Boolean = true,
    val priorityWeight: Double = 0.1,
    val publicFileBonus: Int = 5
) 