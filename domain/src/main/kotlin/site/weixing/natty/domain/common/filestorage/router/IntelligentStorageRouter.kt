package site.weixing.natty.domain.common.filestorage.router

import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy

/**
 * 智能存储路由器接口
 * 根据文件特征和系统状态动态选择最优存储策略
 */
interface IntelligentStorageRouter {
    
    /**
     * 选择最优存储策略
     * @param context 文件上传上下文
     * @return 选中的存储策略
     */
    fun selectOptimalStrategy(context: FileUploadContext): Mono<FileStorageStrategy>
    
    /**
     * 获取可用的回退策略列表
     * @param primaryStrategy 主要策略
     * @return 回退策略列表
     */
    fun getFallbackStrategies(primaryStrategy: FileStorageStrategy): List<FileStorageStrategy>
    
    /**
     * 验证策略是否可用
     * @param strategy 存储策略
     * @return 是否可用
     */
    fun isStrategyAvailable(strategy: FileStorageStrategy): Mono<Boolean>
    
    /**
     * 获取策略健康状态
     * @param provider 存储提供商
     * @return 健康分数（0-100）
     */
    fun getStrategyHealthScore(provider: StorageProvider): Mono<Int>
} 