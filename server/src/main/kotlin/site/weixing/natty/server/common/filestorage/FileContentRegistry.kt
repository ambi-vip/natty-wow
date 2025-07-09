package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 文件内容注册表
 * 在内存中临时存储文件内容，用于命令处理和事件处理之间的数据传递
 * 
 * 注意：这是一个简化的内存实现，生产环境应该使用：
 * - Redis缓存
 * - 消息队列的消息体
 * - 临时对象存储
 */
@Component
object FileContentRegistry {
    
    private val logger = KotlinLogging.logger {}
    
    // 临时文件内容存储
    private val tempFileContents = ConcurrentHashMap<String, TempFileContent>()
    
    // 清理线程池
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "file-content-cleanup").apply {
            isDaemon = true
        }
    }
    
    init {
        // 启动定期清理任务
        cleanupExecutor.scheduleAtFixedRate(
            { cleanupExpiredContent() },
            1, // 初始延迟1分钟
            5, // 每5分钟执行一次
            TimeUnit.MINUTES
        )
    }
    
    /**
     * 存储临时文件内容
     * @param tempFileId 临时文件ID
     * @param content 文件内容
     * @param fileName 文件名（用于日志）
     */
    fun storeTempFileContent(tempFileId: String, content: ByteArray, fileName: String) {
        val tempFileContent = TempFileContent(
            content = content,
            fileName = fileName,
            storageTime = System.currentTimeMillis()
        )
        
        tempFileContents[tempFileId] = tempFileContent
        
        logger.debug { "存储临时文件内容: $fileName (ID: $tempFileId, 大小: ${content.size} 字节)" }
    }
    
    /**
     * 获取临时文件内容
     * @param tempFileId 临时文件ID
     * @return 文件内容，如果不存在则返回null
     */
    fun getTempFileContent(tempFileId: String): ByteArray? {
        val tempFileContent = tempFileContents[tempFileId]
        if (tempFileContent != null) {
            logger.debug { "获取临时文件内容: ${tempFileContent.fileName} (ID: $tempFileId)" }
        } else {
            logger.warn { "临时文件内容不存在: ID: $tempFileId" }
        }
        return tempFileContent?.content
    }
    
    /**
     * 移除临时文件内容
     * @param tempFileId 临时文件ID
     */
    fun removeTempFileContent(tempFileId: String) {
        val removed = tempFileContents.remove(tempFileId)
        if (removed != null) {
            logger.debug { "移除临时文件内容: ${removed.fileName} (ID: $tempFileId)" }
        } else {
            logger.warn { "尝试移除不存在的临时文件内容: ID: $tempFileId" }
        }
    }
    
    /**
     * 检查临时文件内容是否存在
     * @param tempFileId 临时文件ID
     * @return 是否存在
     */
    fun exists(tempFileId: String): Boolean {
        return tempFileContents.containsKey(tempFileId)
    }
    
    /**
     * 获取当前存储的临时文件数量
     */
    fun getStoredFileCount(): Int {
        return tempFileContents.size
    }
    
    /**
     * 获取存储的总内容大小（字节）
     */
    fun getTotalStorageSize(): Long {
        return tempFileContents.values.sumOf { it.content.size.toLong() }
    }
    
    /**
     * 清理过期的临时文件内容
     * 默认超过30分钟的内容会被清理
     */
    private fun cleanupExpiredContent() {
        val currentTime = System.currentTimeMillis()
        val expirationTime = 30 * 60 * 1000L // 30分钟
        
        val expiredIds = tempFileContents.entries
            .filter { (currentTime - it.value.storageTime) > expirationTime }
            .map { it.key }
        
        if (expiredIds.isNotEmpty()) {
            expiredIds.forEach { tempFileId ->
                val removed = tempFileContents.remove(tempFileId)
                if (removed != null) {
                    logger.info { "清理过期临时文件内容: ${removed.fileName} (ID: $tempFileId)" }
                }
            }
            
            logger.info { "清理完成，移除了 ${expiredIds.size} 个过期的临时文件内容" }
        }
    }
    
    /**
     * 手动清理所有临时文件内容（测试用）
     */
    fun clearAll() {
        val count = tempFileContents.size
        tempFileContents.clear()
        logger.info { "清理了所有临时文件内容，共 $count 个" }
    }
    
    /**
     * 关闭清理线程池
     */
    fun shutdown() {
        cleanupExecutor.shutdown()
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cleanupExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
    
    /**
     * 临时文件内容数据类
     */
    private data class TempFileContent(
        val content: ByteArray,
        val fileName: String,
        val storageTime: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TempFileContent

            if (!content.contentEquals(other.content)) return false
            if (fileName != other.fileName) return false
            if (storageTime != other.storageTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + storageTime.hashCode()
            return result
        }
    }
} 