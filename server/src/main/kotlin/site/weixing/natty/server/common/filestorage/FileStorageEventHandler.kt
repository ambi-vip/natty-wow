package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.command.CommandGateway
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.FileDeleted
import site.weixing.natty.api.common.filestorage.file.FileMoved
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory

/**
 * 文件存储事件处理器
 * 监听文件相关事件，处理文件删除和移动操作
 * 注意：文件上传现在在聚合根中直接处理，不再需要事件驱动
 */
@Component
class FileStorageEventHandler(
    private val fileStorageService: FileStorageService,
    private val commandGateway: CommandGateway,
    private val strategyFactory: FileStorageStrategyFactory
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 处理文件上传事件
     * 注意：现在文件上传在聚合根中直接处理，此方法仅用于日志记录
     */
    @OnEvent
    fun onFileUploaded(event: FileUploaded): Mono<Void> {
        logger.info { 
            "文件上传完成: ${event.fileName} -> ${event.actualStoragePath} " +
            "(提供商: ${event.storageProvider}, 大小: ${event.fileSize} bytes)" 
        }
        return Mono.empty()
    }
    
    /**
     * 处理文件删除事件
     */
    @OnEvent
    fun onFileDeleted(event: FileDeleted): Mono<Void> {
        logger.info { "处理文件删除事件: ${event.fileName} -> ${event.storagePath}" }
        
        return try {
            val storageConfig = getDefaultLocalStorageConfig()
            val strategy = strategyFactory.createStrategy(StorageProvider.LOCAL, storageConfig)

            fileStorageService.deleteFile(event.storagePath)
                .doOnSuccess { deleted ->
                    if (deleted) {
                        logger.info { "文件物理删除成功: ${event.fileName}" }
                    } else {
                        logger.warn { "文件不存在，删除跳过: ${event.fileName}" }
                    }
                }
                .doOnError { error ->
                    logger.error(error) { "文件物理删除失败: ${event.fileName}" }
                }
                .then()
        } catch (e: Exception) {
            logger.error(e) { "处理文件删除事件失败: ${event.fileName}" }
            Mono.empty()
        }
    }
    
    /**
     * 处理文件移动事件
     */
    @OnEvent
    fun onFileMoved(event: FileMoved): Mono<Void> {
        logger.info { "处理文件移动事件: ${event.fileName} (${event.oldStoragePath} -> ${event.newStoragePath})" }
        
        return try {
            val storageConfig = getDefaultLocalStorageConfig()
            val strategy = strategyFactory.createStrategy(StorageProvider.LOCAL, storageConfig)
            
            // 使用存储策略的moveFile方法
            fileStorageService.moveFile(event.oldStoragePath, event.newStoragePath)
                .doOnSuccess { moved ->
                    if (moved) {
                        logger.info { "文件物理移动成功: ${event.fileName}" }
                    } else {
                        logger.warn { "文件移动失败: ${event.fileName}" }
                    }
                }
                .doOnError { error ->
                    logger.error(error) { "文件物理移动失败: ${event.fileName}" }
                }
                .then()
        } catch (e: Exception) {
            logger.error(e) { "处理文件移动事件失败: ${event.fileName}" }
            Mono.empty()
        }
    }
    
    /**
     * 获取默认的本地存储配置
     * TODO: 这里应该从配置服务或存储配置聚合中获取
     */
    private fun getDefaultLocalStorageConfig(): Map<String, Any> {
        // 获取当前项目根目录
        val projectRoot = System.getProperty("user.dir")
        
        // 检查是否在测试环境
        val isTestEnvironment = System.getProperty("spring.profiles.active")?.contains("test") == true ||
                                System.getenv("SPRING_PROFILES_ACTIVE")?.contains("test") == true
        
        val baseDirectory = if (isTestEnvironment) {
            "$projectRoot/storage/files-test"
        } else {
            "$projectRoot/storage/files"
        }
        
        return mapOf(
            "baseDirectory" to baseDirectory,
            "maxFileSize" to (100 * 1024 * 1024L), // 100MB
            "allowedContentTypes" to emptyList<String>(),
            "enableChecksumValidation" to true,
            "urlPrefix" to "file://"
        )
    }
    

} 