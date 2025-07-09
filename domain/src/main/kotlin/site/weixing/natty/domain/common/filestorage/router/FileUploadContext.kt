package site.weixing.natty.domain.common.filestorage.router

import java.time.LocalDateTime

/**
 * 文件上传上下文
 * 包含智能路由决策所需的所有信息
 */
data class FileUploadContext(
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploaderId: String,
    val folderId: String,
    val isPublic: Boolean,
    val expectedAccessPattern: AccessPattern = AccessPattern.WARM,
    val priorityLevel: Priority = Priority.NORMAL,
    val geoLocation: String? = null,
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap(),
    val uploadTime: LocalDateTime = LocalDateTime.now(),
    val replaceIfExists: Boolean = false
) {
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String? {
        return fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }
    
    /**
     * 是否为大文件（>100MB）
     */
    fun isLargeFile(): Boolean {
        return fileSize > 100 * 1024 * 1024L
    }
    
    /**
     * 是否为小文件（<1MB）
     */
    fun isSmallFile(): Boolean {
        return fileSize < 1024 * 1024L
    }
    
    /**
     * 是否为图片文件
     */
    fun isImageFile(): Boolean {
        return contentType.startsWith("image/")
    }
    
    /**
     * 是否为视频文件
     */
    fun isVideoFile(): Boolean {
        return contentType.startsWith("video/")
    }
    
    /**
     * 是否为文档文件
     */
    fun isDocumentFile(): Boolean {
        return contentType.startsWith("application/") || 
               contentType.startsWith("text/") ||
               contentType.contains("pdf") ||
               contentType.contains("word") ||
               contentType.contains("excel") ||
               contentType.contains("powerpoint")
    }
    
    /**
     * 是否需要CDN加速
     */
    fun needsCdnAcceleration(): Boolean {
        return isPublic && (isImageFile() || isVideoFile() || expectedAccessPattern == AccessPattern.HOT)
    }
    
    /**
     * 获取估计的访问频率评分（0-100）
     */
    fun getEstimatedAccessScore(): Int {
        var score = when (expectedAccessPattern) {
            AccessPattern.HOT -> 80
            AccessPattern.WARM -> 50
            AccessPattern.COLD -> 20
        }
        
        // 公开文件通常访问频率更高
        if (isPublic) score += 15
        
        // 图片和视频文件通常访问频率更高
        if (isImageFile() || isVideoFile()) score += 10
        
        return minOf(score, 100)
    }
} 