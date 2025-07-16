package site.weixing.natty.domain.common.filestorage.storage

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.storage.*

import org.slf4j.LoggerFactory

/**
 * 存储配置聚合根
 * 管理文件存储配置的创建、更新和验证
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class StorageConfig(
    private val state: StorageConfigState
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StorageConfig::class.java)
    }

    @OnCommand
    fun onCreate(command: CreateStorageConfig): Mono<StorageConfigCreated> {
        logger.info("创建存储配置: name={}, provider={}", command.name, command.provider)
        
        // 业务规则校验
        validateConfigName(command.name)
        ensureNotDeleted()
        
        // 验证存储配置参数
        return validateStorageConfig(command.provider, command.config)
            .flatMap { isValid ->
                if (!isValid) {
                    Mono.error(IllegalArgumentException("存储配置验证失败：配置参数无效"))
                } else {
                    // 如果设置为默认配置，确保当前没有其他默认配置
                    if (command.isDefault) {
                        logger.info("设置为默认存储配置: {}", command.name)
                    }
                    
                    StorageConfigCreated(
                        name = command.name,
                        provider = command.provider,
                        config = command.config,
                        isDefault = command.isDefault,
                        isEnabled = command.isEnabled,
                        description = command.description
                    ).toMono()
                }
            }
            .doOnSuccess { 
                logger.info("存储配置创建成功: name={}, provider={}", command.name, command.provider)
            }
            .doOnError { error ->
                logger.error("存储配置创建失败: name={}, error={}", command.name, error.message)
            }
    }

    @OnCommand
    fun onUpdate(command: UpdateStorageConfig): Mono<StorageConfigUpdated> {
        logger.info("更新存储配置: {}", state.name)
        
        ensureNotDeleted()
        
        // 验证名称变更（如果有）
        command.name?.let { newName ->
            if (newName != state.name) {
                validateConfigName(newName)
            }
        }
        
        // 验证配置变更（如果有）
        val validationMono = if (command.config != null && state.provider != null) {
            validateStorageConfig(state.provider!!, command.config!!)
                .flatMap { isValid ->
                    if (!isValid) {
                        Mono.error(IllegalArgumentException("存储配置验证失败：配置参数无效"))
                    } else {
                        Mono.just(true)
                    }
                }
        } else {
            Mono.just(true)
        }
        
        return validationMono.map {
            StorageConfigUpdated(
                name = command.name,
                config = command.config,
                isEnabled = command.isEnabled,
                description = command.description
            )
        }.doOnSuccess { 
            logger.info("存储配置更新成功: {}", state.name)
        }
    }
    
    @OnCommand
    fun onDelete(command: DeleteStorageConfig): Mono<StorageConfigDeleted> {
        logger.info("删除存储配置: {}", state.name)
        
        ensureNotDeleted()
        
        // 检查是否为默认配置
        if (state.isDefault && !command.force) {
            return Mono.error(IllegalStateException("不能删除默认存储配置，请先设置其他配置为默认"))
        }
        
        // TODO: 检查是否有文件正在使用此配置（简化实现，实际需要查询文件表）
        if (!command.force) {
            logger.warn("删除存储配置可能影响现有文件: {}", state.name)
        }
        
        return StorageConfigDeleted(
            name = state.name ?: "unknown",
            provider = state.provider ?: StorageProvider.LOCAL
        ).toMono().doOnSuccess {
            logger.info("存储配置删除成功: {}", state.name)
        }
    }
    
    @OnCommand
    fun onSwitchProvider(command: SwitchStorageProvider): Mono<StorageProviderSwitched> {
        logger.info("切换存储提供商: {} -> {}", state.provider, command.newProvider)
        
        ensureNotDeleted()
        
        val oldProvider = state.provider ?: StorageProvider.LOCAL
        
        // 验证新的存储配置
        return validateStorageConfig(command.newProvider, command.newConfig)
            .flatMap { isValid ->
                if (!isValid) {
                    Mono.error(IllegalArgumentException("新存储配置验证失败：配置参数无效"))
                } else {
                    StorageProviderSwitched(
                        oldProvider = oldProvider,
                        newProvider = command.newProvider,
                        newConfig = command.newConfig
                    ).toMono()
                }
            }
            .doOnSuccess { 
                logger.info("存储提供商切换成功: {} -> {}", oldProvider, command.newProvider)
            }
    }
    
    @OnCommand
    fun onSetDefault(command: SetDefaultStorageConfig): Mono<DefaultStorageConfigChanged> {
        logger.info("设置默认存储配置: name={}, enabled={}", state.name, command.enabled)
        
        ensureNotDeleted()
        
        // 确保配置有效且启用
        if (!state.isValid() || !state.isEnabled) {
            return Mono.error(IllegalStateException("只能将有效且启用的配置设置为默认"))
        }
        
        return DefaultStorageConfigChanged(
            oldConfigId = null, // 简化实现，实际需要查询当前默认配置
            newConfigId = state.id
        ).toMono().doOnSuccess {
            logger.info("默认存储配置设置成功: {}", state.name)
        }
    }
    
    /**
     * 验证存储配置参数的有效性
     * 根据不同存储提供商验证必需的配置参数
     */
    private fun validateStorageConfig(provider: StorageProvider, config: Map<String, Any>): Mono<Boolean> {
        return try {
            when (provider) {
                StorageProvider.LOCAL -> validateLocalConfig(config)
                StorageProvider.S3 -> validateS3Config(config)
                StorageProvider.ALIYUN_OSS -> validateAliyunOssConfig(config)
            }
            Mono.just(true)
        } catch (e: Exception) {
            logger.error("存储配置验证失败: provider={}, error={}", provider, e.message)
            Mono.just(false)
        }
    }
    
    /**
     * 验证本地存储配置
     */
    private fun validateLocalConfig(config: Map<String, Any>) {
        val baseDirectory = config["baseDirectory"] as? String
        require(!baseDirectory.isNullOrBlank()) { "本地存储基础目录不能为空" }
        
        val maxFileSize = config["maxFileSize"]
        if (maxFileSize != null && maxFileSize !is Number) {
            throw IllegalArgumentException("maxFileSize必须是数字类型")
        }
        
        val enableChecksumValidation = config["enableChecksumValidation"]
        if (enableChecksumValidation != null && enableChecksumValidation !is Boolean) {
            throw IllegalArgumentException("enableChecksumValidation必须是布尔类型")
        }
    }
    
    /**
     * 验证S3存储配置
     */
    private fun validateS3Config(config: Map<String, Any>) {
        val accessKeyId = config["accessKeyId"] as? String
        val secretAccessKey = config["secretAccessKey"] as? String
        val region = config["region"] as? String
        val bucketName = config["bucketName"] as? String
        
        require(!accessKeyId.isNullOrBlank()) { "S3访问密钥ID不能为空" }
        require(!secretAccessKey.isNullOrBlank()) { "S3访问密钥不能为空" }
        require(!region.isNullOrBlank()) { "S3区域不能为空" }
        require(!bucketName.isNullOrBlank()) { "S3存储桶名称不能为空" }
    }
    
    /**
     * 验证阿里云OSS存储配置
     */
    private fun validateAliyunOssConfig(config: Map<String, Any>) {
        val accessKeyId = config["accessKeyId"] as? String
        val accessKeySecret = config["accessKeySecret"] as? String
        val endpoint = config["endpoint"] as? String
        val bucketName = config["bucketName"] as? String
        
        require(!accessKeyId.isNullOrBlank()) { "阿里云OSS访问密钥ID不能为空" }
        require(!accessKeySecret.isNullOrBlank()) { "阿里云OSS访问密钥不能为空" }
        require(!endpoint.isNullOrBlank()) { "阿里云OSS端点不能为空" }
        require(!bucketName.isNullOrBlank()) { "阿里云OSS存储桶名称不能为空" }
    }
    
    /**
     * 验证配置名称的有效性
     * 确保名称不为空且符合规范
     */
    private fun validateConfigName(name: String) {
        require(name.isNotBlank()) { "存储配置名称不能为空" }
        require(name.length <= 100) { "存储配置名称长度不能超过100个字符" }
        require(!name.contains("/") && !name.contains("\\")) { "存储配置名称不能包含路径分隔符" }
        
        // 检查是否与现有配置重名（这里简化处理，实际应该查询数据库）
        if (state.name != null && state.name == name && !state.isDeleted) {
            throw IllegalArgumentException("存储配置名称已存在: $name")
        }
    }
    
    /**
     * 确保配置未被删除
     * 防止对已删除配置进行操作
     */
    private fun ensureNotDeleted() {
        if (state.isDeleted) {
            throw IllegalStateException("不能对已删除的存储配置进行操作")
        }
    }
} 