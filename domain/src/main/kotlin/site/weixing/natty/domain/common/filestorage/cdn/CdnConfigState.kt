package site.weixing.natty.domain.common.filestorage.cdn

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.filestorage.cdn.CdnProvider
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigCreated
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigUpdated
import site.weixing.natty.api.common.filestorage.cdn.CdnEnabled
import site.weixing.natty.api.common.filestorage.cdn.CdnDisabled
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigDeleted
import site.weixing.natty.api.common.filestorage.cdn.DefaultCdnConfigChanged
import java.time.LocalDateTime

/**
 * CDN配置状态类
 * 管理CDN配置的完整状态信息
 */
class CdnConfigState(override val id: String) : Identifier {

    var name: String? = null
        private set
    
    var domain: String? = null
        private set
    
    var provider: CdnProvider? = null
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
    fun onCdnConfigCreated(event: CdnConfigCreated) {
        this.name = event.name
        this.domain = event.domain
        this.provider = event.provider
        this.config = event.config
        this.isDefault = event.isDefault
        this.isEnabled = event.isEnabled
        this.description = event.description
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onCdnConfigUpdated(event: CdnConfigUpdated) {
        event.name?.let { this.name = it }
        event.domain?.let { this.domain = it }
        event.config?.let { this.config = it }
        event.isEnabled?.let { this.isEnabled = it }
        event.description?.let { this.description = it }
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onCdnEnabled(event: CdnEnabled) {
        this.isEnabled = true
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onCdnDisabled(event: CdnDisabled) {
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onCdnConfigDeleted(event: CdnConfigDeleted) {
        this.isDeleted = true
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    @OnSourcing
    fun onDefaultCdnConfigChanged(event: DefaultCdnConfigChanged) {
        this.isDefault = (event.newConfigId == this.id)
        this.updatedAt = LocalDateTime.now()
    }
    
    /**
     * 获取CDN提供商的显示名称
     */
    fun getProviderDisplayName(): String? {
        return provider?.displayName
    }
    
    /**
     * 获取完整的CDN URL
     */
    fun getCdnUrl(filePath: String): String? {
        return if (isEnabled && !domain.isNullOrBlank()) {
            val cleanPath = filePath.removePrefix("/")
            "https://$domain/$cleanPath"
        } else null
    }
    
    /**
     * 是否为自定义CDN
     */
    fun isCustomCdn(): Boolean {
        return provider == CdnProvider.CUSTOM
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
               !domain.isNullOrBlank() &&
               provider != null && 
               !isDeleted
    }
    
    /**
     * 获取域名
     */
    fun getDomainWithProtocol(): String? {
        return domain?.let { 
            if (it.startsWith("http://") || it.startsWith("https://")) {
                it
            } else {
                "https://$it"
            }
        }
    }
} 