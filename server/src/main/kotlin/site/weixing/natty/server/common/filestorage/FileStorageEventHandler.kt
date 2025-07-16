package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ahoo.wow.api.annotation.OnEvent
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

            fileStorageService.deleteFile(event.storagePath)
                .doOnSuccess { deleted ->
                    logger.info { "文件物理删除成功: ${event.fileName}" }
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

            // 使用存储策略的moveFile方法
            fileStorageService.moveFile(event.oldStoragePath, event.newStoragePath)
                .doOnSuccess { moved ->
                    logger.info { "文件物理移动成功: ${event.fileName}" }
                    if (moved) {
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
    

} 