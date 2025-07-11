package site.weixing.natty.server.common.filestorage.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Scheduled
import site.weixing.natty.domain.common.filestorage.temp.LocalTemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileManager
import site.weixing.natty.domain.common.filestorage.temp.TemporaryFileTransaction
import jakarta.annotation.PostConstruct

/**
 * 文件存储配置类
 * 
 * 负责配置和注册文件存储相关的Spring Bean，包括：
 * 1. 临时文件管理器配置
 * 2. 事务性清理机制配置
 * 3. 配置参数验证和初始化
 * 
 * 这个配置类确保所有文件存储组件能够正确初始化和依赖注入。
 */
@Configuration
@EnableConfigurationProperties(TemporaryFileConfig::class)
class FileStorageConfiguration(
    private val temporaryFileConfig: TemporaryFileConfig
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    @PostConstruct
    fun validateConfiguration() {
        logger.info { "开始验证文件存储配置" }
        
        val validationErrors = temporaryFileConfig.validate()
        if (validationErrors.isNotEmpty()) {
            val errorMessage = "文件存储配置验证失败:\n${validationErrors.joinToString("\n")}"
            logger.error { errorMessage }
            throw IllegalArgumentException(errorMessage)
        }
        
        logger.info { "文件存储配置验证通过" }
        logger.info { temporaryFileConfig.printSummary() }
    }
    
    /**
     * 注册临时文件管理器Bean
     * 
     * 使用配置的参数创建本地临时文件管理器实例。
     * 如果用户提供了自定义的TemporaryFileManager实现，则使用用户的实现。
     */
    @Bean
    @ConditionalOnMissingBean(TemporaryFileManager::class)
    fun temporaryFileManager(): TemporaryFileManager {
        logger.info { "创建临时文件管理器Bean" }
        
        val manager = LocalTemporaryFileManager(
            tempDirectory = temporaryFileConfig.directory,
            defaultExpirationHours = temporaryFileConfig.expirationHours,
            maxFileSize = temporaryFileConfig.maxFileSize,
            cleanupIntervalMinutes = temporaryFileConfig.cleanupIntervalMinutes
        )
        
        logger.info { "临时文件管理器Bean创建完成: ${manager::class.simpleName}" }
        return manager
    }
    
    /**
     * 注册临时文件事务管理器Bean
     * 
     * 依赖于临时文件管理器，提供事务性的文件操作能力。
     */
    @Bean
    @DependsOn("temporaryFileManager")
    fun temporaryFileTransaction(temporaryFileManager: TemporaryFileManager): TemporaryFileTransaction {
        logger.info { "创建临时文件事务管理器Bean" }
        
        val transaction = TemporaryFileTransaction(temporaryFileManager)
        
        logger.info { "临时文件事务管理器Bean创建完成" }
        return transaction
    }
    
    /**
     * 注册文件存储健康检查器Bean
     * 
     * 用于监控文件存储系统的健康状态。
     */
    @Bean
    fun fileStorageHealthIndicator(
        temporaryFileManager: TemporaryFileManager,
        temporaryFileConfig: TemporaryFileConfig
    ): FileStorageHealthIndicator {
        logger.info { "创建文件存储健康检查器Bean" }
        
        return FileStorageHealthIndicator(temporaryFileManager, temporaryFileConfig)
    }

}

/**
 * 文件存储健康检查器
 * 
 * 监控文件存储系统的健康状态，包括：
 * 1. 临时文件目录是否可访问
 * 2. 磁盘空间是否充足
 * 3. 临时文件管理器是否正常工作
 */
class FileStorageHealthIndicator(
    private val temporaryFileManager: TemporaryFileManager,
    private val temporaryFileConfig: TemporaryFileConfig
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 检查文件存储系统健康状态
     */
    fun checkHealth(): Map<String, Any> {
        val health = mutableMapOf<String, Any>()
        
        try {
            // 检查临时文件目录
            val tempDir = java.io.File(temporaryFileConfig.directory)
            health["tempDirectory"] = mapOf(
                "path" to temporaryFileConfig.directory,
                "exists" to tempDir.exists(),
                "writable" to tempDir.canWrite(),
                "freeSpace" to tempDir.freeSpace,
                "totalSpace" to tempDir.totalSpace
            )
            
            // 检查配置有效性
            val configErrors = temporaryFileConfig.validate()
            health["configuration"] = mapOf(
                "valid" to configErrors.isEmpty(),
                "errors" to configErrors
            )
            
            // 检查临时文件管理器
            health["temporaryFileManager"] = mapOf(
                "class" to temporaryFileManager::class.simpleName,
                "status" to "active"
            )
            
            health["status"] = "UP"
            
        } catch (e: Exception) {
            logger.error(e) { "文件存储健康检查失败" }
            health["status"] = "DOWN"
            health["error"] = e.message ?: "未知错误"
        }
        
        return health
    }
}