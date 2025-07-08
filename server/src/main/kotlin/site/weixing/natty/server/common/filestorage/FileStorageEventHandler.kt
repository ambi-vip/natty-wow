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
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategyFactory
import site.weixing.natty.domain.common.filestorage.service.LocalFileStorageService
import site.weixing.natty.domain.common.filestorage.service.TempFileStorageService
import java.io.ByteArrayInputStream

/**
 * 文件存储事件处理器
 * 监听文件相关事件并调用实际的存储策略进行物理文件操作
 */
@Component
class FileStorageEventHandler(
    private val localFileStorageService: LocalFileStorageService,
    private val tempFileStorageService: TempFileStorageService,
    private val commandGateway: CommandGateway
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * 处理文件上传事件
     * 当File聚合根发布FileUploaded事件后，执行实际的文件存储操作
     */
    @OnEvent
    fun onFileUploaded(event: FileUploaded): Mono<Void> {
        logger.info { "处理文件上传事件: ${event.fileName} -> ${event.storagePath}" }
        
        return try {
            // 获取默认的本地存储配置
            val storageConfig = getDefaultLocalStorageConfig()
            
            // 创建存储策略
            val strategy = FileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL, storageConfig)
            
            // 从文件内容注册表获取文件内容
            val fileContent = FileContentRegistry.getTempFileContent(event.tempFileId)
            
            if (fileContent == null) {
                logger.error { "临时文件内容不存在: ${event.tempFileId} for ${event.fileName}" }
                return@fromCallable
            }
            
            val inputStream = ByteArrayInputStream(fileContent)
            
            // 执行实际的文件存储
            localFileStorageService.uploadFile(
                strategy = strategy,
                filePath = event.storagePath,
                inputStream = inputStream,
                contentType = event.contentType,
                fileSize = event.fileSize
            )
            .doOnSuccess { storageInfo ->
                logger.info { "文件物理存储成功: ${event.fileName} (存储路径: ${event.storagePath})" }
                // 清理临时文件内容
                FileContentRegistry.removeTempFileContent(event.tempFileId)
                tempFileStorageService.deleteTempFile(event.tempFileId)
                logger.debug { "临时文件已清理: ${event.tempFileId}" }
            }
            .doOnError { error ->
                logger.error(error) { "文件物理存储失败: ${event.fileName}" }
                // 保留临时文件以便重试或调试
                logger.warn { "临时文件保留用于重试: ${event.tempFileId}" }
            }
            .then()
            
        } catch (e: Exception) {
            logger.error(e) { "处理文件上传事件失败: ${event.fileName}" }
            Mono.empty()
        }
    }
    
    /**
     * 处理文件删除事件
     */
    @OnEvent
    fun onFileDeleted(event: FileDeleted): Mono<Void> {
        logger.info { "处理文件删除事件: ${event.fileName} -> ${event.storagePath}" }
        
        return try {
            val storageConfig = getDefaultLocalStorageConfig()
            val strategy = FileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL, storageConfig)
            
            localFileStorageService.deleteFile(strategy, event.storagePath)
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
            val strategy = FileStorageStrategyFactory.createStrategy(StorageProvider.LOCAL, storageConfig)
            
            // 使用存储策略的moveFile方法
            strategy.moveFile(event.oldStoragePath, event.newStoragePath)
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
        return mapOf(
            "baseDirectory" to "/tmp/natty-files", // 临时目录，实际应该配置到正式的存储目录
            "maxFileSize" to (100 * 1024 * 1024L), // 100MB
            "allowedContentTypes" to emptyList<String>(),
            "enableChecksumValidation" to true,
            "urlPrefix" to "file://"
        )
    }
    

} 