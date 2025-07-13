package site.weixing.natty.api.common.filestorage.file

import java.time.Duration

/**
 * API层处理选项值对象
 * 轻量级处理配置，用于API层传递处理需求
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
     * 最大处理时间（分钟）
     */
    val maxProcessingTimeMinutes: Long = 5
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
}