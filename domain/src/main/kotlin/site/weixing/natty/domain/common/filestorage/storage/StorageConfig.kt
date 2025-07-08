package site.weixing.natty.domain.common.filestorage.storage

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.storage.CreateStorageConfig
import site.weixing.natty.api.common.filestorage.storage.StorageConfigCreated
import site.weixing.natty.api.common.filestorage.storage.StorageConfigUpdated
import site.weixing.natty.api.common.filestorage.storage.StorageProviderSwitched
import site.weixing.natty.api.common.filestorage.storage.StorageConfigDeleted
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.api.common.filestorage.config.LocalStorageParams
import site.weixing.natty.api.common.filestorage.config.S3StorageParams
import site.weixing.natty.api.common.filestorage.config.AliyunOssStorageParams

/**
 * 存储配置聚合根
 * 管理不同存储提供商的配置
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class StorageConfig(private val state: StorageConfigState) {

    @OnCommand
    fun onCreate(command: CreateStorageConfig): Mono<StorageConfigCreated> {
        // 业务规则校验
        require(command.name.isNotBlank()) { "配置名称不能为空" }
        require(command.config.isNotEmpty()) { "配置参数不能为空" }
        
        // 验证配置参数
        validateStorageConfig(command.provider, command.config)
        
        return StorageConfigCreated(
            name = command.name,
            provider = command.provider,
            config = command.config,
            isDefault = command.isDefault,
            isEnabled = command.isEnabled,
            description = command.description
        ).toMono()
    }
    
    /**
     * 验证存储配置参数
     */
    private fun validateStorageConfig(provider: StorageProvider, config: Map<String, Any>) {
        when (provider) {
            StorageProvider.LOCAL -> validateLocalStorageConfig(config)
            StorageProvider.S3 -> validateS3StorageConfig(config)
            StorageProvider.ALIYUN_OSS -> validateAliyunOssStorageConfig(config)
        }
    }
    
    /**
     * 验证本地存储配置
     */
    private fun validateLocalStorageConfig(config: Map<String, Any>) {
        try {
            val params = LocalStorageParams.fromMap(config)
            
            // 验证基础路径
            require(params.basePath.isNotBlank()) { "本地存储基础路径不能为空" }
            
            // 验证文件大小限制
            require(params.maxFileSize > 0) { "文件最大大小必须大于0" }
            
            // 验证目录深度
            require(params.maxDirectoryDepth > 0) { "目录最大深度必须大于0" }
            
            // 验证临时文件过期时间
            require(params.tempFileExpiryHours > 0) { "临时文件过期时间必须大于0小时" }
            
            // 验证压缩阈值
            if (params.enableCompression) {
                require(params.compressionThreshold > 0) { "压缩阈值必须大于0" }
            }
            
        } catch (e: Exception) {
            throw IllegalArgumentException("本地存储配置参数无效: ${e.message}", e)
        }
    }
    
    /**
     * 验证S3存储配置
     */
    private fun validateS3StorageConfig(config: Map<String, Any>) {
        try {
            val params = S3StorageParams.fromMap(config)
            
            require(params.accessKeyId.isNotBlank()) { "S3访问密钥ID不能为空" }
            require(params.secretAccessKey.isNotBlank()) { "S3访问密钥不能为空" }
            require(params.bucketName.isNotBlank()) { "S3存储桶名称不能为空" }
            require(params.region.isNotBlank()) { "S3区域不能为空" }
            
            // 验证存储桶名称格式
            require(isValidS3BucketName(params.bucketName)) { 
                "S3存储桶名称格式无效: ${params.bucketName}" 
            }
            
        } catch (e: Exception) {
            throw IllegalArgumentException("S3存储配置参数无效: ${e.message}", e)
        }
    }
    
    /**
     * 验证阿里云OSS存储配置
     */
    private fun validateAliyunOssStorageConfig(config: Map<String, Any>) {
        try {
            val params = AliyunOssStorageParams.fromMap(config)
            
            require(params.accessKeyId.isNotBlank()) { "阿里云OSS访问密钥ID不能为空" }
            require(params.accessKeySecret.isNotBlank()) { "阿里云OSS访问密钥不能为空" }
            require(params.bucketName.isNotBlank()) { "阿里云OSS存储桶名称不能为空" }
            require(params.endpoint.isNotBlank()) { "阿里云OSS端点地址不能为空" }
            
            // 验证端点地址格式
            require(isValidUrl(params.endpoint)) { 
                "阿里云OSS端点地址格式无效: ${params.endpoint}" 
            }
            
            // 验证超时设置
            require(params.connectTimeout > 0) { "连接超时时间必须大于0" }
            require(params.socketTimeout > 0) { "Socket超时时间必须大于0" }
            
        } catch (e: Exception) {
            throw IllegalArgumentException("阿里云OSS存储配置参数无效: ${e.message}", e)
        }
    }
    
    /**
     * 验证S3存储桶名称格式
     */
    private fun isValidS3BucketName(bucketName: String): Boolean {
        // S3存储桶名称规则
        val regex = Regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$")
        return bucketName.length in 3..63 && 
               regex.matches(bucketName) &&
               !bucketName.contains("..") &&
               !bucketName.contains(".-") &&
               !bucketName.contains("-.")
    }
    
    /**
     * 验证URL格式
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取配置摘要信息
     */
    private fun getConfigSummary(provider: StorageProvider, config: Map<String, Any>): String {
        return when (provider) {
            StorageProvider.LOCAL -> {
                val basePath = config["basePath"] as? String ?: "未知路径"
                "本地存储: $basePath"
            }
            StorageProvider.S3 -> {
                val bucketName = config["bucketName"] as? String ?: "未知桶"
                val region = config["region"] as? String ?: "未知区域"
                "S3存储: $bucketName@$region"
            }
            StorageProvider.ALIYUN_OSS -> {
                val bucketName = config["bucketName"] as? String ?: "未知桶"
                val endpoint = config["endpoint"] as? String ?: "未知端点"
                "阿里云OSS: $bucketName@$endpoint"
            }
        }
    }
} 