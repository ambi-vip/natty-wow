package site.weixing.natty.domain.common.filestorage.router

/**
 * 优先级枚举
 * 定义文件上传和存储的优先级级别
 */
enum class Priority(
    val level: Int,
    val displayName: String,
    val description: String
) {
    /**
     * 低优先级
     * 适合批量上传、后台处理
     */
    LOW(
        level = 1,
        displayName = "低优先级",
        description = "非紧急文件，可以稍后处理"
    ),
    
    /**
     * 正常优先级
     * 默认优先级，适合大部分文件
     */
    NORMAL(
        level = 2,
        displayName = "正常优先级", 
        description = "标准优先级，正常处理"
    ),
    
    /**
     * 高优先级
     * 适合重要文件、实时需求
     */
    HIGH(
        level = 3,
        displayName = "高优先级",
        description = "重要文件，优先处理"
    ),
    
    /**
     * 紧急优先级
     * 适合关键业务文件
     */
    CRITICAL(
        level = 4,
        displayName = "紧急优先级",
        description = "关键业务文件，立即处理"
    );
    
    companion object {
        /**
         * 根据级别获取优先级
         */
        fun fromLevel(level: Int): Priority? {
            return values().find { it.level == level }
        }
        
        /**
         * 根据文件特征推断优先级
         */
        fun inferFromContext(isPublic: Boolean, fileSize: Long, tags: List<String>): Priority {
            return when {
                // 包含紧急标签
                tags.any { it.lowercase().contains("urgent") || it.lowercase().contains("critical") } -> CRITICAL
                
                // 公开的小文件通常需要高优先级（如头像、logo）
                isPublic && fileSize < 10 * 1024 * 1024L -> HIGH  // < 10MB
                
                // 包含重要标签
                tags.any { it.lowercase().contains("important") || it.lowercase().contains("priority") } -> HIGH
                
                // 大文件通常是低优先级（除非特别标记）
                fileSize > 1024 * 1024 * 1024L -> LOW  // > 1GB
                
                // 默认正常优先级
                else -> NORMAL
            }
        }
    }
} 