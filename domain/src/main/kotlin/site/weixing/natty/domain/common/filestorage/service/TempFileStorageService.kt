package site.weixing.natty.domain.common.filestorage.service

import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 临时文件存储服务
 * 用于在命令处理和事件处理之间传递文件内容
 * 
 * 注意：这是一个简化的内存实现，生产环境应该使用：
 * - Redis缓存
 * - 临时对象存储
 * - 数据库BLOB存储
 */
@Service
class TempFileStorageService {
    
    private val tempFiles = ConcurrentHashMap<String, TempFileInfo>()
    
    /**
     * 存储临时文件
     * @param fileContent 文件内容
     * @param fileName 文件名
     * @param contentType 内容类型
     * @return 临时文件ID
     */
    fun storeTempFile(
        fileContent: ByteArray,
        fileName: String,
        contentType: String
    ): String {
        val tempFileId = UUID.randomUUID().toString()
        val tempFileInfo = TempFileInfo(
            id = tempFileId,
            fileName = fileName,
            contentType = contentType,
            content = fileContent,
            createdAt = LocalDateTime.now()
        )
        
        tempFiles[tempFileId] = tempFileInfo
        
        // 启动清理任务（简化实现，生产环境应该使用定时任务）
        scheduleCleanup(tempFileId)
        
        return tempFileId
    }
    
    /**
     * 获取临时文件内容
     * @param tempFileId 临时文件ID
     * @return 文件输入流
     */
    fun getTempFileContent(tempFileId: String): InputStream? {
        val tempFileInfo = tempFiles[tempFileId]
        return tempFileInfo?.let { ByteArrayInputStream(it.content) }
    }
    
    /**
     * 获取临时文件信息
     * @param tempFileId 临时文件ID
     * @return 临时文件信息
     */
    fun getTempFileInfo(tempFileId: String): TempFileInfo? {
        return tempFiles[tempFileId]
    }
    
    /**
     * 删除临时文件
     * @param tempFileId 临时文件ID
     */
    fun deleteTempFile(tempFileId: String) {
        tempFiles.remove(tempFileId)
    }
    
    /**
     * 检查临时文件是否存在
     * @param tempFileId 临时文件ID
     * @return 是否存在
     */
    fun exists(tempFileId: String): Boolean {
        return tempFiles.containsKey(tempFileId)
    }
    
    /**
     * 获取临时文件数量
     */
    fun getTempFileCount(): Int {
        return tempFiles.size
    }
    
    /**
     * 清理过期的临时文件
     */
    fun cleanupExpiredFiles() {
        val now = LocalDateTime.now()
        val expiredIds = tempFiles.entries
            .filter { ChronoUnit.HOURS.between(it.value.createdAt, now) > 1 } // 1小时过期
            .map { it.key }
        
        expiredIds.forEach { tempFiles.remove(it) }
        
        if (expiredIds.isNotEmpty()) {
            println("清理了 ${expiredIds.size} 个过期临时文件")
        }
    }
    
    /**
     * 安排清理任务（简化实现）
     */
    private fun scheduleCleanup(tempFileId: String) {
        // 简化实现：在新线程中延迟删除
        // 生产环境应该使用定时任务或TTL机制
        Thread {
            Thread.sleep(3600000) // 1小时后清理
            deleteTempFile(tempFileId)
        }.start()
    }
}

/**
 * 临时文件信息
 */
data class TempFileInfo(
    val id: String,
    val fileName: String,
    val contentType: String,
    val content: ByteArray,
    val createdAt: LocalDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TempFileInfo

        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (!content.contentEquals(other.content)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
} 