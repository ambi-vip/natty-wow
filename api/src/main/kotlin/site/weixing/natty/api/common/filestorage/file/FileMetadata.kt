package site.weixing.natty.api.common.filestorage.file

import java.time.LocalDateTime

/**
 * 文件元数据值对象
 */
data class FileMetadata(
    val originalName: String,
    val contentType: String,
    val size: Long,
    val checksum: String? = null,
    val encoding: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,
    val uploaderId: String? = null,
    val uploadTime: LocalDateTime? = null,
    val lastModifiedTime: LocalDateTime? = null,
    val customProperties: Map<String, String> = emptyMap()
) {
    /**
     * 是否为图片文件
     */
    fun isImage(): Boolean {
        return contentType.startsWith("image/")
    }
    
    /**
     * 是否为视频文件
     */
    fun isVideo(): Boolean {
        return contentType.startsWith("video/")
    }
    
    /**
     * 是否为音频文件
     */
    fun isAudio(): Boolean {
        return contentType.startsWith("audio/")
    }
    
    /**
     * 是否为文档文件
     */
    fun isDocument(): Boolean {
        return contentType.startsWith("application/") || 
               contentType.startsWith("text/") ||
               contentType.contains("pdf") ||
               contentType.contains("word") ||
               contentType.contains("excel") ||
               contentType.contains("powerpoint")
    }
} 