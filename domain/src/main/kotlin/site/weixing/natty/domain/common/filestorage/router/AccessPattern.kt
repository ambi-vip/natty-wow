package site.weixing.natty.domain.common.filestorage.router

/**
 * 访问模式枚举
 * 定义文件的预期访问频率模式
 */
enum class AccessPattern(
    val code: String,
    val displayName: String,
    val description: String,
    val preferredStorageType: String
) {
    /**
     * 热数据 - 频繁访问
     * 适合CDN、本地SSD存储
     */
    HOT(
        code = "hot",
        displayName = "热数据",
        description = "频繁访问的文件，需要最快的访问速度",
        preferredStorageType = "LOCAL_SSD"
    ),
    
    /**
     * 温数据 - 正常访问
     * 适合标准存储、云存储
     */
    WARM(
        code = "warm",
        displayName = "温数据",
        description = "正常访问频率的文件，平衡性能和成本",
        preferredStorageType = "STANDARD"
    ),
    
    /**
     * 冷数据 - 低频访问
     * 适合归档存储、冷存储
     */
    COLD(
        code = "cold",
        displayName = "冷数据",
        description = "低频访问的文件，优化存储成本",
        preferredStorageType = "ARCHIVE"
    );
    
    companion object {
        /**
         * 根据代码获取访问模式
         */
        fun fromCode(code: String): AccessPattern? {
            return values().find { it.code == code }
        }
        
        /**
         * 根据文件类型和公开性推断访问模式
         */
        fun inferFromFile(contentType: String, isPublic: Boolean, fileSize: Long): AccessPattern {
            return when {
                // 公开的图片/视频通常是热数据
                isPublic && (contentType.startsWith("image/") || contentType.startsWith("video/")) -> HOT
                
                // 大文件通常是冷数据
                fileSize > 1024 * 1024 * 1024L -> COLD  // > 1GB
                
                // 文档类文件通常是温数据
                contentType.startsWith("application/") || contentType.startsWith("text/") -> WARM
                
                // 默认为温数据
                else -> WARM
            }
        }
    }
} 