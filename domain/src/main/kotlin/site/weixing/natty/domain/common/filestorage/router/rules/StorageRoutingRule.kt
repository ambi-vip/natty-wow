package site.weixing.natty.domain.common.filestorage.router.rules

import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy

/**
 * 存储路由规则接口
 * 定义智能路由决策规则的通用接口
 */
interface StorageRoutingRule {
    
    /**
     * 评估给定上下文的路由决策
     * @param context 文件上传上下文
     * @return 路由决策
     */
    fun evaluate(context: FileUploadContext): Mono<RoutingDecision>
    
    /**
     * 获取规则权重
     * 权重越高，在综合决策中影响越大
     * @return 权重值 (0-100)
     */
    fun getWeight(): Int
    
    /**
     * 规则是否适用于给定上下文
     * @param context 文件上传上下文  
     * @return 是否适用
     */
    fun isApplicable(context: FileUploadContext): Boolean = true
    
    /**
     * 获取规则名称
     * @return 规则名称
     */
    fun getRuleName(): String = this::class.simpleName ?: "UnknownRule"
    
    /**
     * 获取规则描述
     * @return 规则描述
     */
    fun getDescription(): String = "存储路由规则"
}

/**
 * 路由决策数据类
 * 包含路由决策的完整信息
 */
data class RoutingDecision(
    val strategy: FileStorageStrategy,
    val score: Int, // 评分 (0-100)
    val reason: String, // 决策原因
    val ruleType: String, // 规则类型
    val confidence: Double = score / 100.0, // 置信度 (0.0-1.0)
    val metadata: Map<String, Any> = emptyMap() // 额外元数据
) {
    
    /**
     * 是否为高置信度决策
     */
    fun isHighConfidence(): Boolean = confidence >= 0.8
    
    /**
     * 是否为低置信度决策
     */
    fun isLowConfidence(): Boolean = confidence <= 0.4
    
    /**
     * 获取决策等级
     */
    fun getDecisionLevel(): DecisionLevel {
        return when {
            confidence >= 0.9 -> DecisionLevel.EXCELLENT
            confidence >= 0.7 -> DecisionLevel.GOOD  
            confidence >= 0.5 -> DecisionLevel.ACCEPTABLE
            confidence >= 0.3 -> DecisionLevel.POOR
            else -> DecisionLevel.UNACCEPTABLE
        }
    }
    
    /**
     * 与另一个决策比较
     */
    fun isBetterThan(other: RoutingDecision): Boolean {
        return this.score > other.score
    }
}

/**
 * 决策等级枚举
 */
enum class DecisionLevel(val displayName: String) {
    EXCELLENT("优秀"),
    GOOD("良好"),
    ACCEPTABLE("可接受"),
    POOR("较差"),
    UNACCEPTABLE("不可接受")
} 