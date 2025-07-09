package site.weixing.natty.domain.common.filestorage.router

import io.swagger.v3.oas.annotations.servers.Server
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.rules.*
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import java.time.LocalDateTime

/**
 * 智能存储路由器实现类
 * 整合多种路由规则，使用加权评分机制选择最优存储策略
 */
@Component
class IntelligentStorageRouterImpl(
    private val strategyFactory: FileStorageStrategyFactory,
    private val configuration: IntelligentStorageRouterConfiguration = IntelligentStorageRouterConfiguration()
) : IntelligentStorageRouter {

    private val logger = LoggerFactory.getLogger(IntelligentStorageRouterImpl::class.java)
    
    // 路由规则列表
    private val routingRules: List<StorageRoutingRule> by lazy {
        initializeRoutingRules()
    }

    override fun selectOptimalStrategy(context: FileUploadContext): Mono<FileStorageStrategy> {
        return getAvailableStrategies()
            .flatMap { availableStrategies ->
                if (availableStrategies.isEmpty()) {
                    Mono.error(IllegalStateException("没有可用的存储策略"))
                } else {
                    evaluateAllRules(context, availableStrategies)
                        .collectList()
                        .map { decisions -> selectBestDecision(decisions, context) }
                        .map { bestDecision ->
                            logRoutingDecision(context, bestDecision)
                            bestDecision.strategy
                        }
                }
            }
    }

    override fun getFallbackStrategies(primaryStrategy: FileStorageStrategy): List<FileStorageStrategy> {
        return try {
            getAvailableStrategies()
                .map { strategies ->
                    strategies.values.filter { it != primaryStrategy }
                        .sortedByDescending { strategy ->
                            // 按可靠性排序：LOCAL > S3 > ALIYUN_OSS
                            when (strategy::class.simpleName) {
                                "LocalFileStorageStrategy" -> 100
                                "S3FileStorageStrategy" -> 80
                                "AliyunOssFileStorageStrategy" -> 70
                                else -> 50
                            }
                        }
                }
                .block() ?: emptyList()
        } catch (e: Exception) {
            logger.warn("获取回退策略失败", e)
            emptyList()
        }
    }

    override fun isStrategyAvailable(strategy: FileStorageStrategy): Mono<Boolean> {
        return strategy.isAvailable()
            .doOnError { error ->
                logger.warn("检查存储策略可用性时出错: ${strategy::class.simpleName}", error)
            }
            .onErrorReturn(false)
    }

    override fun getStrategyHealthScore(provider: StorageProvider): Mono<Int> {
        return getAvailableStrategies()
            .flatMap { strategies ->
                val strategy = strategies[provider]
                if (strategy != null) {
                    strategy.isAvailable()
                        .map { available ->
                            if (available) {
                                when (provider) {
                                    StorageProvider.LOCAL -> 95
                                    StorageProvider.S3 -> 85
                                    StorageProvider.ALIYUN_OSS -> 80
                                }
                            } else {
                                0
                            }
                        }
                } else {
                    Mono.just(0)
                }
            }
    }

    fun getRoutingMetrics(): Mono<RoutingMetrics> {
        return getAvailableStrategies()
            .map { strategies ->
                RoutingMetrics(
                    availableStrategies = strategies.keys.toList(),
                    totalRules = routingRules.size,
                    lastUpdated = LocalDateTime.now(),
                    configuration = configuration
                )
            }
    }

    /**
     * 获取可用的存储策略映射
     */
    private fun getAvailableStrategies(): Mono<Map<StorageProvider, FileStorageStrategy>> {
        return Flux.fromArray(StorageProvider.entries.toTypedArray())
            .flatMap { provider ->
                try {
                    val strategy = strategyFactory.createStrategy(provider, emptyMap())
                    strategy.isAvailable()
                        .filter { available -> available }
                        .map { provider to strategy }
                        .onErrorResume { error ->
                            logger.debug("存储策略 $provider 不可用: ${error.message}")
                            Mono.empty()
                        }
                } catch (e: Exception) {
                    logger.debug("创建存储策略 $provider 失败: ${e.message}")
                    Mono.empty()
                }
            }
            .collectMap({ it.first }, { it.second })
    }

    /**
     * 评估所有适用的路由规则
     */
    private fun evaluateAllRules(
        context: FileUploadContext, 
        availableStrategies: Map<StorageProvider, FileStorageStrategy>
    ): Flux<RoutingDecision> {
        return Flux.fromIterable(routingRules)
            .filter { rule -> rule.isApplicable(context) }
            .flatMap { rule ->
                rule.evaluate(context)
                    .doOnError { error ->
                        logger.warn("路由规则 ${rule.getRuleName()} 评估失败", error)
                    }
                    .onErrorResume { 
                        // 规则评估失败时返回默认决策
                        createDefaultDecision(availableStrategies, rule.getRuleName())
                    }
            }
    }

    /**
     * 选择最佳决策
     */
    private fun selectBestDecision(decisions: List<RoutingDecision>, context: FileUploadContext): RoutingDecision {
        if (decisions.isEmpty()) {
            throw IllegalStateException("没有可用的路由决策")
        }
        
        // 计算加权分数
        val weightedDecisions = decisions.map { decision ->
            val rule = routingRules.find { it.getRuleName().contains(decision.ruleType, ignoreCase = true) }
            val weight = rule?.getWeight() ?: 50
            val weightedScore = decision.score * (weight / 100.0)
            
            decision to weightedScore
        }
        
        // 选择加权分数最高的决策
        val bestDecision = weightedDecisions.maxByOrNull { it.second }?.first
            ?: decisions.maxByOrNull { it.score }!!
        
        // 如果最佳决策分数太低，尝试使用回退策略
        return if (bestDecision.isLowConfidence() && configuration.enableFallback) {
            logger.info("最佳决策置信度较低 (${bestDecision.confidence})，尝试回退策略")
            createFallbackDecision(bestDecision, context)
        } else {
            bestDecision
        }
    }

    /**
     * 创建回退决策
     */
    private fun createFallbackDecision(
        originalDecision: RoutingDecision, 
        context: FileUploadContext
    ): RoutingDecision {
        val fallbackStrategies = getFallbackStrategies(originalDecision.strategy)
        
        return if (fallbackStrategies.isNotEmpty()) {
            RoutingDecision(
                strategy = fallbackStrategies.first(),
                score = 75, // 回退策略固定分数
                reason = "原决策置信度过低，使用回退策略: ${fallbackStrategies.first()::class.simpleName}",
                ruleType = "FALLBACK",
                confidence = 0.75
            )
        } else {
            // 如果没有回退策略，返回原决策
            originalDecision
        }
    }

    /**
     * 创建默认决策（当规则评估失败时）
     */
    private fun createDefaultDecision(
        availableStrategies: Map<StorageProvider, FileStorageStrategy>,
        ruleName: String
    ): Mono<RoutingDecision> {
        return Mono.fromCallable {
            val defaultStrategy = availableStrategies[StorageProvider.LOCAL]
                ?: availableStrategies.values.first()
            
            RoutingDecision(
                strategy = defaultStrategy,
                score = 60,
                reason = "规则 $ruleName 评估失败，使用默认策略",
                ruleType = "DEFAULT"
            )
        }
    }

    /**
     * 初始化路由规则
     */
    private fun initializeRoutingRules(): List<StorageRoutingRule> {
        return try {
            getAvailableStrategies().map { availableStrategies ->
                listOf(
                    FileSizeBasedRule(availableStrategies),
                    ContentTypeBasedRule(availableStrategies),
                    AccessPatternBasedRule(availableStrategies)
                )
            }.block() ?: emptyList()
        } catch (e: Exception) {
            logger.warn("初始化路由规则失败", e)
            emptyList()
        }
    }

    /**
     * 记录路由决策日志
     */
    private fun logRoutingDecision(context: FileUploadContext, decision: RoutingDecision) {
        val logLevel = when (decision.getDecisionLevel()) {
            DecisionLevel.EXCELLENT, DecisionLevel.GOOD -> "INFO"
            DecisionLevel.ACCEPTABLE -> "DEBUG"
            else -> "WARN"
        }
        
        val message = """
            |智能路由决策完成:
            |  文件: ${context.fileName} (${context.fileSize} bytes)
            |  策略: ${decision.strategy::class.simpleName}
            |  分数: ${decision.score} (置信度: ${"%.2f".format(decision.confidence)})
            |  原因: ${decision.reason}
            |  规则: ${decision.ruleType}
        """.trimMargin()
        
        when (logLevel) {
            "INFO" -> logger.info(message)
            "DEBUG" -> logger.debug(message)
            "WARN" -> logger.warn(message)
        }
    }
}

/**
 * 智能存储路由器配置
 */
data class IntelligentStorageRouterConfiguration(
    val enableFallback: Boolean = true,
    val fallbackThreshold: Double = 0.4,
    val enableMetrics: Boolean = true,
    val logDecisions: Boolean = true,
    val maxRetries: Int = 3
)

/**
 * 路由指标数据类
 */
data class RoutingMetrics(
    val availableStrategies: List<StorageProvider>,
    val totalRules: Int,
    val lastUpdated: LocalDateTime,
    val configuration: IntelligentStorageRouterConfiguration
) 