package site.weixing.natty.server.common.filestorage.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

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
    


}