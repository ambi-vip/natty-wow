package site.weixing.natty.domain.common.filestorage.processing

import java.time.Duration

/**
 * 处理选项值对象
 * 轻量级处理配置，替代复杂的管道配置
 */
data class ProcessingOptions(
    /**
     * 是否需要加密
     */
    val requireEncryption: Boolean = false,
    
    /**
     * 是否启用压缩
     */
    val enableCompression: Boolean = false,
    
    /**
     * 是否生成缩略图
     */
    val generateThumbnail: Boolean = false,
    
    /**
     * 自定义处理器列表
     */
    val customProcessors: List<String> = emptyList(),
    
    /**
     * 最大处理时间
     */
    val maxProcessingTime: Duration = Duration.ofMinutes(5)
) {
    
    /**
     * 检查是否需要任何处理
     */
    fun requiresProcessing(): Boolean {
        return requireEncryption || enableCompression || generateThumbnail || customProcessors.isNotEmpty()
    }
    
    /**
     * 获取所有启用的处理器名称
     */
    fun getEnabledProcessors(): List<String> {
        val processors = mutableListOf<String>()
        
        if (requireEncryption) processors.add("EncryptionProcessor")
        if (enableCompression) processors.add("CompressionProcessor")
        if (generateThumbnail) processors.add("ThumbnailProcessor")
        
        processors.addAll(customProcessors)
        
        return processors
    }
    
    /**
     * 创建默认的处理选项（无处理）
     */
    companion object {
        fun none(): ProcessingOptions {
            return ProcessingOptions()
        }
        
        /**
         * 创建基础安全处理选项（只加密）
         */
        fun secureOnly(): ProcessingOptions {
            return ProcessingOptions(requireEncryption = true)
        }
        
        /**
         * 创建图片处理选项（加密+缩略图）
         */
        fun forImage(isPublic: Boolean): ProcessingOptions {
            return ProcessingOptions(
                requireEncryption = !isPublic,
                generateThumbnail = isPublic
            )
        }
        
        /**
         * 创建文档处理选项（加密+压缩）
         */
        fun forDocument(isPublic: Boolean): ProcessingOptions {
            return ProcessingOptions(
                requireEncryption = !isPublic,
                enableCompression = true
            )
        }
    }
}