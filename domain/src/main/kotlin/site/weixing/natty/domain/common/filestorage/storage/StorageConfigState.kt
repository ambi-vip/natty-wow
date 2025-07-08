package site.weixing.natty.domain.common.filestorage.storage

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.api.common.filestorage.storage.StorageConfigCreated
import site.weixing.natty.api.common.filestorage.storage.StorageConfigUpdated
import site.weixing.natty.api.common.filestorage.storage.StorageProviderSwitched
import site.weixing.natty.api.common.filestorage.storage.StorageConfigDeleted
import site.weixing.natty.api.common.filestorage.storage.DefaultStorageConfigChanged
import java.time.LocalDateTime

/**
 * 存储配置状态类
 * 管理存储配置的完整状态信息
 */
class StorageConfigState(override val id: String) : Identifier {

    var name: String? = null
        private set
    
    var provider: StorageProvider? = null
        private set
    
    var config: Map<String, Any> = emptyMap()
        private set
    
    var isDefault: Boolean = false
        private set
    
    var isEnabled: Boolean = true
        private set
    
    var description: String? = null
        private set
    
    var createdAt: LocalDateTime? = null
        private set
    
    var updatedAt: LocalDateTime? = null
        private set
    
    var isDeleted: Boolean = false
        private set

    @OnSourcing
    fun onStorageConfigCreated(event: StorageConfigCreated) {
        this.name = event.name
        this.provider = event.provider
        this.config = event.config
        this.isDefault = event.isDefault
        this.isEnabled = event.isEnabled
        this.description = event.description
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onStorageConfigUpdated(event: StorageConfigUpdated) {
        event.name?.let { this.name = it }
        event.config?.let { this.config = it }
        event.isEnabled?.let { this.isEnabled = it }
        event.description?.let { this.description = it }
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onStorageProviderSwitched(event: StorageProviderSwitched) {
        this.provider = event.newProvider
        this.config = event.newConfig
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onStorageConfigDeleted(event: StorageConfigDeleted) {
        this.isDeleted = true
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onDefaultStorageConfigChanged(event: DefaultStorageConfigChanged) {
        this.isDefault = (event.newConfigId == this.id)
        this.updatedAt = LocalDateTime.now()
    }
    
    /**
     * 获取存储提供商的显示名称
     */
    fun getProviderDisplayName(): String? {
        return provider?.displayName
    }
    
    /**
     * 是否为云存储
     */
    fun isCloudStorage(): Boolean {
        return provider == StorageProvider.S3 || provider == StorageProvider.ALIYUN_OSS
    }
    
    /**
     * 是否为本地存储
     */
    fun isLocalStorage(): Boolean {
        return provider == StorageProvider.LOCAL
    }
    
    /**
     * 获取配置参数的字符串表示
     */
    fun getConfigString(): String {
        return config.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }
    
    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return !name.isNullOrBlank() && 
               provider != null && 
               config.isNotEmpty() && 
               !isDeleted
    }
} 