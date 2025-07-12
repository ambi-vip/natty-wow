package site.weixing.natty.domain.common.filestorage.file

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.filestorage.file.FileStatus
import site.weixing.natty.api.common.filestorage.file.FileMetadata
import site.weixing.natty.api.common.filestorage.file.FileUploaded
import site.weixing.natty.api.common.filestorage.file.FileUpdated
import site.weixing.natty.api.common.filestorage.file.FileDeleted
import site.weixing.natty.api.common.filestorage.file.FileMoved
import site.weixing.natty.api.common.filestorage.file.FileCopied
import site.weixing.natty.api.common.filestorage.file.FileStatusChanged
import java.time.LocalDateTime

/**
 * 文件状态类
 * 管理文件的完整生命周期状态
 */
class FileState(override val id: String) : Identifier {

    var fileName: String? = null
        private set
    
    var folderId: String? = null
        private set
    
    var uploaderId: String? = null
        private set
    
    var size: Long = 0
        private set
    
    var contentType: String? = null
        private set
    
    var storageInfo: StorageInfo? = null
        private set
    
    var metadata: FileMetadata? = null
        private set
    
    var status: FileStatus = FileStatus.UPLOADING
        private set
    
    var versions: MutableList<FileVersion> = mutableListOf()
        private set
    
    var tags: List<String> = emptyList()
        private set
    
    var isPublic: Boolean = false
        private set
    
    var customMetadata: Map<String, String> = emptyMap()
        private set
    
    var createdAt: Long = System.currentTimeMillis()
        private set
    
    var updatedAt: Long = System.currentTimeMillis()
        private set

    @OnSourcing
    fun onFileUploaded(event: FileUploaded) {
        this.fileName = event.fileName
        this.folderId = event.folderId
        this.uploaderId = event.uploaderId
        this.size = event.fileSize
        this.contentType = event.contentType
        this.storageInfo = StorageInfo.local(event.storagePath, event.checksum)
        this.isPublic = event.isPublic
        this.tags = event.tags
        this.customMetadata = event.customMetadata
        this.status = FileStatus.ACTIVE
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
        
        // 创建初始版本
        event.checksum?.let { checksum ->
            this.storageInfo?.let { storage ->
                val initialVersion = FileVersion.initial(
                    storageInfo = storage,
                    size = event.fileSize,
                    checksum = checksum,
                    uploaderId = event.uploaderId
                )
                this.versions.add(initialVersion)
            }
        }
    }

    @OnSourcing
    fun onFileUpdated(event: FileUpdated) {
        event.fileName?.let { this.fileName = it }
        event.isPublic?.let { this.isPublic = it }
        event.tags?.let { this.tags = it }
        event.customMetadata?.let { this.customMetadata = it }
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileDeleted(event: FileDeleted) {
        this.status = FileStatus.DELETED
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileMoved(event: FileMoved) {
        this.folderId = event.newFolderId
        this.storageInfo = this.storageInfo?.copy(storagePath = event.newStoragePath)
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileCopied(event: FileCopied) {
        // 文件复制事件处理逻辑（如果需要在原文件状态中记录）
        this.updatedAt = System.currentTimeMillis()
    }

    @OnSourcing
    fun onFileStatusChanged(event: FileStatusChanged) {
        this.status = event.newStatus
        this.updatedAt = System.currentTimeMillis()
    }
    
    /**
     * 获取当前活跃版本
     */
    fun getCurrentVersion(): FileVersion? {
        return versions.find { it.isActive }
    }
    
    /**
     * 获取最新版本号
     */
    fun getLatestVersionNumber(): Int {
        return versions.maxOfOrNull { it.version } ?: 0
    }
    
    /**
     * 添加新版本
     */
    fun addVersion(version: FileVersion) {
        // 停用之前的活跃版本
        versions.forEach { v ->
            if (v.isActive) {
                val index = versions.indexOf(v)
                versions[index] = v.deactivate()
            }
        }
        // 添加新版本
        versions.add(version)
    }
    
    /**
     * 是否支持版本控制
     */
    fun isVersioningEnabled(): Boolean {
        return versions.size > 1
    }
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String? {
        return fileName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * 是否为图片文件
     */
    fun isImage(): Boolean {
        return contentType?.startsWith("image/") == true
    }
    
    /**
     * 是否为文档文件
     */
    fun isDocument(): Boolean {
        return contentType?.let { type ->
            type.startsWith("application/") || 
            type.startsWith("text/") ||
            type.contains("pdf") ||
            type.contains("word") ||
            type.contains("excel") ||
            type.contains("powerpoint")
        } == true
    }
} 