package site.weixing.natty.api.common.filestorage.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * 本地存储配置参数
 */
data class LocalStorageParams(
    @field:NotBlank(message = "存储基础路径不能为空")
    val basePath: String,
    
    @field:Min(value = 1, message = "文件最大大小必须大于0")
    val maxFileSize: Long = 100 * 1024 * 1024, // 默认100MB
    
    val allowedExtensions: Set<String> = emptySet(), // 空集合表示允许所有类型
    
    val enableVersioning: Boolean = false,
    
    val enableThumbnail: Boolean = true,
    
    val cleanupTempFiles: Boolean = true,
    
    @field:Min(value = 1, message = "目录最大深度必须大于0")
    val maxDirectoryDepth: Int = 10,
    
    val tempFileExpiryHours: Int = 24,
    
    val enableCompression: Boolean = false,
    
    val compressionThreshold: Long = 1024 * 1024 // 1MB
) {
    companion object {
        fun fromMap(params: Map<String, Any>): LocalStorageParams {
            return LocalStorageParams(
                basePath = params["basePath"] as? String ?: "",
                maxFileSize = (params["maxFileSize"] as? Number)?.toLong() ?: 100 * 1024 * 1024,
                allowedExtensions = (params["allowedExtensions"] as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet(),
                enableVersioning = params["enableVersioning"] as? Boolean ?: false,
                enableThumbnail = params["enableThumbnail"] as? Boolean ?: true,
                cleanupTempFiles = params["cleanupTempFiles"] as? Boolean ?: true,
                maxDirectoryDepth = (params["maxDirectoryDepth"] as? Number)?.toInt() ?: 10,
                tempFileExpiryHours = (params["tempFileExpiryHours"] as? Number)?.toInt() ?: 24,
                enableCompression = params["enableCompression"] as? Boolean ?: false,
                compressionThreshold = (params["compressionThreshold"] as? Number)?.toLong() ?: 1024 * 1024
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "basePath" to basePath,
            "maxFileSize" to maxFileSize,
            "allowedExtensions" to allowedExtensions.toList(),
            "enableVersioning" to enableVersioning,
            "enableThumbnail" to enableThumbnail,
            "cleanupTempFiles" to cleanupTempFiles,
            "maxDirectoryDepth" to maxDirectoryDepth,
            "tempFileExpiryHours" to tempFileExpiryHours,
            "enableCompression" to enableCompression,
            "compressionThreshold" to compressionThreshold
        )
    }
} 