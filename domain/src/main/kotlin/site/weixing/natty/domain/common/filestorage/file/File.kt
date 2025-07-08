package site.weixing.natty.domain.common.filestorage.file

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.FileUpdated
import site.weixing.natty.api.common.filestorage.file.FileDeleted
import site.weixing.natty.api.common.filestorage.file.FileMoved
import site.weixing.natty.api.common.filestorage.file.FileCopied
import site.weixing.natty.api.common.filestorage.file.FileStatus
import java.security.MessageDigest
import java.util.UUID

/**
 * 文件聚合根
 * 管理文件的完整生命周期
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class File(private val state: FileState) {

    @OnCommand
    fun onUpload(command: UploadFile): Mono<FileUploaded> {
        // 业务规则校验

        // 验证文件名格式
        validateFileName(command.fileName)
        
        // 验证文件大小
        require(command.fileContent.size.toLong() == command.fileSize) { 
            "文件内容大小与声明大小不匹配" 
        }
        
        // 计算文件校验和（如果未提供）
        val checksum = command.checksum ?: calculateChecksum(command.fileContent)
        
        // 验证校验和（如果提供了）
        command.checksum?.let { providedChecksum ->
            val calculatedChecksum = calculateChecksum(command.fileContent)
            require(providedChecksum == calculatedChecksum) { 
                "文件校验和不匹配，文件可能已损坏" 
            }
        }
        
        // 生成存储路径
        val storagePath = generateStoragePath(command.folderId, command.fileName)
        
        // 生成临时文件ID（实际的文件内容存储将在事件处理器中处理）
        val tempFileId = UUID.randomUUID().toString()
        
        return FileUploaded(
            fileName = command.fileName,
            folderId = command.folderId,
            uploaderId = command.uploaderId,
            fileSize = command.fileSize,
            contentType = command.contentType,
            storagePath = storagePath,
            checksum = checksum,
            isPublic = command.isPublic,
            tags = command.tags,
            customMetadata = command.customMetadata,
            tempFileId = tempFileId
        ).toMono()
    }
    
    /**
     * 验证文件名是否合法
     */
    private fun validateFileName(fileName: String) {
        // 文件名不能包含特殊字符
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        require(fileName.none { it in invalidChars }) { 
            "文件名包含非法字符: ${invalidChars.joinToString("")}" 
        }
        
        // 文件名长度限制
        require(fileName.length <= 255) { "文件名长度不能超过255个字符" }
        
        // 文件名不能以点开头或结尾
        require(!fileName.startsWith(".") && !fileName.endsWith(".")) { 
            "文件名不能以点开头或结尾" 
        }
    }
    
    /**
     * 计算文件SHA-256校验和
     */
    private fun calculateChecksum(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成存储路径
     */
    private fun generateStoragePath(folderId: String, fileName: String): String {
        val timestamp = System.currentTimeMillis()
        val fileExtension = fileName.substringAfterLast('.', "")
        val baseFileName = fileName.substringBeforeLast('.')
        
        return "folders/$folderId/${timestamp}_${baseFileName}.${fileExtension}"
    }
    
    /**
     * 验证文件类型是否被允许
     */
    private fun validateFileType(contentType: String, allowedTypes: Set<String>): Boolean {
        if (allowedTypes.isEmpty()) return true // 空集合表示允许所有类型
        
        return allowedTypes.any { allowedType ->
            when {
                allowedType.endsWith("/*") -> contentType.startsWith(allowedType.removeSuffix("/*"))
                else -> contentType == allowedType
            }
        }
    }
    
    /**
     * 获取文件大小的人类可读格式
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
} 