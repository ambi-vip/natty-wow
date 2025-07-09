package site.weixing.natty.domain.common.filestorage.temp

import java.time.Instant

/**
 * 临时文件引用
 * 
 * 用于在文件上传过程中引用临时存储的文件，避免在内存中传递大文件内容。
 * 临时文件具有生命周期管理，会在指定时间后自动过期清理。
 * 
 * @property referenceId 唯一引用标识符，用于索引临时文件
 * @property originalFileName 原始文件名
 * @property fileSize 文件大小（字节）
 * @property contentType 文件内容类型
 * @property temporaryPath 临时文件存储路径
 * @property createdAt 创建时间
 * @property expiresAt 过期时间
 * @property checksum 文件校验和（可选）
 */
data class TemporaryFileReference(
    val referenceId: String,
    val originalFileName: String,
    val fileSize: Long,
    val contentType: String,
    val temporaryPath: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val checksum: String? = null
) {
    
    /**
     * 检查临时文件是否已过期
     */
    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String? {
        val lastDotIndex = originalFileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < originalFileName.length - 1) {
            originalFileName.substring(lastDotIndex + 1).lowercase()
        } else {
            null
        }
    }
    
    /**
     * 获取不带扩展名的文件名
     */
    fun getFileNameWithoutExtension(): String {
        val lastDotIndex = originalFileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            originalFileName.substring(0, lastDotIndex)
        } else {
            originalFileName
        }
    }
    
    override fun toString(): String {
        return "TemporaryFileReference(referenceId='$referenceId', originalFileName='$originalFileName', " +
                "fileSize=$fileSize, contentType='$contentType', temporaryPath='$temporaryPath', " +
                "createdAt=$createdAt, expiresAt=$expiresAt, checksum=$checksum)"
    }
} 