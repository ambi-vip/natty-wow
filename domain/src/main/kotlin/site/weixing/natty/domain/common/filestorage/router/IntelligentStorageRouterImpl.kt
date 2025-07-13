package site.weixing.natty.domain.common.filestorage.router

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.rules.*
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import me.ahoo.wow.query.dsl.listQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import site.weixing.natty.domain.common.filestorage.storage.StorageConfigState
import java.time.LocalDateTime

/**
 * 智能存储路由器实现类
 * 
 * 基于真实存储配置的智能路由决策系统，整合多种路由规则，
 * 使用加权评分机制选择最优存储策略。
 * 
 * 主要功能：
 * - 动态查询已启用的存储配置
 * - 基于配置创建可用的存储策略实例
 * - 智能路由决策和回退策略支持
 * - 配置驱动的健康状态评估
 * 
 * 架构改进：
 * - 从硬编码枚举改为配置驱动的策略获取
 * - 集成 SnapshotQueryService 实现实时配置查询  
 * - 支持配置变更的动态响应
 * - 完善的错误处理和降级机制
 */
@Component
class IntelligentStorageRouterImpl(
    private val fileStorageStrategyFactory: FileStorageStrategyFactory,
    private val fileStorageService: FileStorageService,
    private val storageConfigQueryService: SnapshotQueryService<StorageConfigState>,
    private val configuration: IntelligentStorageRouterConfiguration = IntelligentStorageRouterConfiguration()
) : IntelligentStorageRouter {

    private val logger = LoggerFactory.getLogger(IntelligentStorageRouterImpl::class.java)
    
    // 路由规则工厂 - 动态创建规则避免启动时阻塞
    private fun createRoutingRules(availableStrategies: Map<StorageProvider, FileStorageStrategy>): List<StorageRoutingRule> {
        return try {
            listOf(
                FileSizeBasedRule(availableStrategies),
                ContentTypeBasedRule(availableStrategies),
                AccessPatternBasedRule(availableStrategies)
            )
        } catch (e: Exception) {
            logger.warn("创建路由规则失败", e)
            emptyList()
        }
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
                            // 基于存储提供商的可靠性和性能进行排序
                            when (strategy.provider) {
                                StorageProvider.LOCAL -> 100 // 本地存储最可靠
                                StorageProvider.S3 -> 80     // S3次之
                                StorageProvider.ALIYUN_OSS -> 70 // 阿里云OSS
                                // 其他未知提供商
                            }
                        }
                }
                .block() ?: run {
                    logger.warn("无法获取动态回退策略，使用默认本地存储")
                    // 如果获取失败，尝试创建默认本地策略作为回退
                    createDefaultLocalStrategy()
                        .map { it.values.toList() }
                        .block() ?: emptyList()
                }
        } catch (e: Exception) {
            logger.warn("获取回退策略失败: ${e.message}", e)
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
                    // 策略存在，检查其可用性
                    strategy.isAvailable()
                        .map { available ->
                            if (available) {
                                // 基于提供商类型和实际配置状态计算健康分数
                                val baseScore = when (provider) {
                                    StorageProvider.LOCAL -> 95  // 本地存储通常最稳定
                                    StorageProvider.S3 -> 85     // S3 具有高可用性
                                    StorageProvider.ALIYUN_OSS -> 80 // 阿里云OSS
                                }
                                logger.debug("存储策略健康分数: {} = {}", provider, baseScore)
                                baseScore
                            } else {
                                logger.warn("存储策略不可用: {}", provider)
                                0
                            }
                        }
                        .onErrorReturn(0) // 如果检查可用性失败，返回0分
                } else {
                    // 策略不存在（可能是配置被禁用或删除）
                    logger.debug("存储策略不存在或未配置: {}", provider)
                    Mono.just(0)
                }
            }
            .onErrorReturn(0) // 如果获取策略失败，返回0分
            .doOnSuccess { score ->
                logger.debug("存储提供商 {} 的健康分数: {}", provider, score)
            }
    }

    fun getRoutingMetrics(): Mono<RoutingMetrics> {
        return getAvailableStrategies()
            .map { strategies ->
                val routingRules = createRoutingRules(strategies)
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
     * 基于真实的存储配置查询已启用的存储策略
     */
    private fun getAvailableStrategies(): Mono<Map<StorageProvider, FileStorageStrategy>> {
        return listQuery {
            condition {
                nestedState()
                "isEnabled" eq true
                "isDeleted" eq false
            }
            limit(20) // 限制查询结果数量
        }.query(storageConfigQueryService)
            .collectList()
            .flatMap { snapshots ->
                if (snapshots.isEmpty()) {
                    logger.warn("未找到任何已启用的存储配置，使用默认本地存储")
                    createDefaultLocalStrategy()
                } else {
                    logger.debug("找到 ${snapshots.size} 个已启用的存储配置")
                    Flux.fromIterable(snapshots)
                        .map { it.state }
                        .filter { validateStorageConfig(it) }
                        .flatMap { configState ->
                            buildStrategyFromConfig(configState)
                                .onErrorResume { error ->
                                    logger.warn("创建存储策略失败: ${configState.provider}, 错误: ${error.message}")
                                    Mono.empty()
                                }
                        }
                        .collectMap({ it.first }, { it.second })
                        .flatMap { strategies ->
                            if (strategies.isEmpty()) {
                                logger.warn("所有配置的存储策略创建失败，回退到默认本地存储")
                                createDefaultLocalStrategy()
                            } else {
                                Mono.just(strategies)
                            }
                        }
                }
            }
            .doOnSuccess { strategies ->
                logger.info("成功加载 ${strategies.size} 个存储策略: ${strategies.keys}")
            }
            .onErrorResume { error ->
                logger.error("查询存储配置失败，回退到默认本地存储: ${error.message}")
                createDefaultLocalStrategy()
            }
    }

    /**
     * 评估所有适用的路由规则
     */
    private fun evaluateAllRules(
        context: FileUploadContext, 
        availableStrategies: Map<StorageProvider, FileStorageStrategy>
    ): Flux<RoutingDecision> {
        // 动态创建规则，使用当前可用的策略映射
        val routingRules = createRoutingRules(availableStrategies)
        
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
        
        // 计算加权分数 - 使用默认权重映射
        val defaultWeights = mapOf(
            "FILE_SIZE" to 80,
            "CONTENT_TYPE" to 70,
            "ACCESS_PATTERN" to 65,
            "DEFAULT" to 50,
            "FALLBACK" to 40
        )
        
        val weightedDecisions = decisions.map { decision ->
            val weight = defaultWeights[decision.ruleType] ?: 50
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
     * 将存储配置转换为策略实例
     */
    private fun buildStrategyFromConfig(configState: StorageConfigState): Mono<Pair<StorageProvider, FileStorageStrategy>> {
        return Mono.fromCallable {
            require(configState.config.isNotEmpty()) { "存储配置参数不能为空" }
            
            val provider = configState.provider
            val strategy = fileStorageStrategyFactory.createStrategy(provider, "1", configState.config)
            
            logger.debug("成功创建存储策略: {} (配置: {})", provider, configState.name)
            provider to strategy
        }
        .flatMap { (provider, strategy) ->
            // 验证策略可用性
            strategy.isAvailable()
                .filter { available -> available }
                .map { provider to strategy }
                .switchIfEmpty(
                    Mono.defer {
                        logger.warn("存储策略不可用: {} (配置: {})", provider, configState.name)
                        Mono.empty()
                    }
                )
        }
        .onErrorMap { error ->
            RuntimeException("创建存储策略失败: ${configState.provider} (配置: ${configState.name})", error)
        }
    }

    /**
     * 验证存储配置有效性
     */
    private fun validateStorageConfig(configState: StorageConfigState): Boolean {
        return try {
            configState.isValid() && 
            configState.provider != null && 
            configState.config.isNotEmpty() &&
            !configState.name.isNullOrBlank()
        } catch (e: Exception) {
            logger.warn("存储配置验证失败: {} - {}", configState.name, e.message)
            false
        }
    }

    /**
     * 创建默认本地存储策略
     */
    private fun createDefaultLocalStrategy(): Mono<Map<StorageProvider, FileStorageStrategy>> {
        return Mono.fromCallable {
            val strategy = fileStorageService.defaultStrategy().block()!!
            mapOf(StorageProvider.LOCAL to strategy)
        }
        .onErrorMap { error ->
            RuntimeException("创建默认本地存储策略失败", error)
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