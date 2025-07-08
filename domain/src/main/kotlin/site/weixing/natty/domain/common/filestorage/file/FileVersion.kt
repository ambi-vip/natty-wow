package site.weixing.natty.domain.common.filestorage.file

import java.time.LocalDateTime

/**
 * 文件版本值对象
 * 用于文件版本控制和历史记录
 */
data class FileVersion(
    val version: Int,
    val storageInfo: StorageInfo,
    val size: Long,
    val checksum: String?,
    val uploaderId: String?,
    val uploadTime: LocalDateTime,
    val comment: String? = null,
    val isActive: Boolean = true
) {
    companion object {
        /**
         * 创建初始版本
         */
        fun initial(
            storageInfo: StorageInfo,
            size: Long,
            checksum: String?,
            uploaderId: String?,
            comment: String? = null
        ): FileVersion {
            return FileVersion(
                version = 1,
                storageInfo = storageInfo,
                size = size,
                checksum = checksum,
                uploaderId = uploaderId,
                uploadTime = LocalDateTime.now(),
                comment = comment ?: "初始版本"
            )
        }
        
        /**
         * 创建新版本
         */
        fun next(
            previousVersion: Int,
            storageInfo: StorageInfo,
            size: Long,
            checksum: String?,
            uploaderId: String?,
            comment: String? = null
        ): FileVersion {
            return FileVersion(
                version = previousVersion + 1,
                storageInfo = storageInfo,
                size = size,
                checksum = checksum,
                uploaderId = uploaderId,
                uploadTime = LocalDateTime.now(),
                comment = comment
            )
        }
    }
    
    /**
     * 是否为初始版本
     */
    fun isInitial(): Boolean {
        return version == 1
    }
    
    /**
     * 获取版本标签
     */
    fun getVersionLabel(): String {
        return "v$version"
    }
    
    /**
     * 停用此版本
     */
    fun deactivate(): FileVersion {
        return copy(isActive = false)
    }
    
    /**
     * 激活此版本
     */
    fun activate(): FileVersion {
        return copy(isActive = true)
    }
} 