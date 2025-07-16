package site.weixing.natty.domain.common.filestorage.cdn

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.cdn.CreateCdnConfig
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigCreated
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigUpdated
import site.weixing.natty.api.common.filestorage.cdn.CdnEnabled
import site.weixing.natty.api.common.filestorage.cdn.CdnDisabled
import site.weixing.natty.api.common.filestorage.cdn.CdnConfigDeleted
import site.weixing.natty.api.common.filestorage.cdn.CdnProvider
import java.net.URL

/**
 * CDN配置聚合根
 * 管理CDN服务的配置和控制
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class CdnConfig(private val state: CdnConfigState) {

    @OnCommand
    fun onCreate(command: CreateCdnConfig): Mono<CdnConfigCreated> {
        // 业务规则校验
        require(command.name.isNotBlank()) { "CDN配置名称不能为空" }
        require(command.domain.isNotBlank()) { "CDN域名不能为空" }
        require(command.config.isNotEmpty()) { "CDN配置参数不能为空" }
        
        // 验证域名格式
        validateDomain(command.domain)
        
        // 验证CDN配置参数
        validateCdnConfig(command.provider, command.config)
        
        return CdnConfigCreated(
            name = command.name,
            domain = command.domain,
            provider = command.provider,
            config = command.config,
            isDefault = command.isDefault,
            isEnabled = command.isEnabled,
            description = command.description
        ).toMono()
    }
    
    /**
     * 验证域名格式
     */
    private fun validateDomain(domain: String) {
        try {
            // 如果域名不包含协议，添加https://前缀进行验证
            val domainWithProtocol = if (domain.startsWith("http://") || domain.startsWith("https://")) {
                domain
            } else {
                "https://$domain"
            }
            
            val url = URL(domainWithProtocol)
            
            // 验证主机名
            require(url.host.isNotBlank()) { "域名主机名不能为空" }
            
            // 验证域名格式
            require(isValidDomainName(url.host)) { "域名格式无效: ${url.host}" }
            
        } catch (e: Exception) {
            throw IllegalArgumentException("域名格式无效: $domain", e)
        }
    }
    
    /**
     * 验证域名格式是否符合规范
     */
    private fun isValidDomainName(domain: String): Boolean {
        // 域名长度限制
        if (domain.length > 253) return false
        
        // 域名格式正则表达式
        val domainRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
        
        return domainRegex.matches(domain) &&
               !domain.startsWith("-") &&
               !domain.endsWith("-") &&
               !domain.contains("..")
    }
    
    /**
     * 验证CDN配置参数
     */
    private fun validateCdnConfig(provider: CdnProvider, config: Map<String, Any>) {
        when (provider) {
            CdnProvider.CLOUDFLARE -> validateCloudflareConfig(config)
            CdnProvider.ALIYUN_CDN -> validateAliyunCdnConfig(config)
            CdnProvider.TENCENT_CDN -> validateTencentCdnConfig(config)
            CdnProvider.QINIU_CDN -> validateQiniuCdnConfig(config)
            CdnProvider.CUSTOM -> validateCustomCdnConfig(config)
        }
    }
    
    /**
     * 验证Cloudflare CDN配置
     */
    private fun validateCloudflareConfig(config: Map<String, Any>) {
        val apiToken = config["apiToken"] as? String
        val zoneId = config["zoneId"] as? String
        
        require(!apiToken.isNullOrBlank()) { "Cloudflare API Token不能为空" }
        require(!zoneId.isNullOrBlank()) { "Cloudflare Zone ID不能为空" }
        
        // 验证Zone ID格式（32位十六进制字符）
        require(zoneId.matches(Regex("^[a-f0-9]{32}$"))) { 
            "Cloudflare Zone ID格式无效: $zoneId" 
        }
    }
    
    /**
     * 验证阿里云CDN配置
     */
    private fun validateAliyunCdnConfig(config: Map<String, Any>) {
        val accessKeyId = config["accessKeyId"] as? String
        val accessKeySecret = config["accessKeySecret"] as? String
        val region = config["region"] as? String
        
        require(!accessKeyId.isNullOrBlank()) { "阿里云CDN访问密钥ID不能为空" }
        require(!accessKeySecret.isNullOrBlank()) { "阿里云CDN访问密钥不能为空" }
        require(!region.isNullOrBlank()) { "阿里云CDN区域不能为空" }
    }
    
    /**
     * 验证腾讯云CDN配置
     */
    private fun validateTencentCdnConfig(config: Map<String, Any>) {
        val secretId = config["secretId"] as? String
        val secretKey = config["secretKey"] as? String
        val region = config["region"] as? String
        
        require(!secretId.isNullOrBlank()) { "腾讯云CDN Secret ID不能为空" }
        require(!secretKey.isNullOrBlank()) { "腾讯云CDN Secret Key不能为空" }
        require(!region.isNullOrBlank()) { "腾讯云CDN区域不能为空" }
    }
    
    /**
     * 验证七牛云CDN配置
     */
    private fun validateQiniuCdnConfig(config: Map<String, Any>) {
        val accessKey = config["accessKey"] as? String
        val secretKey = config["secretKey"] as? String
        
        require(!accessKey.isNullOrBlank()) { "七牛云CDN Access Key不能为空" }
        require(!secretKey.isNullOrBlank()) { "七牛云CDN Secret Key不能为空" }
    }
    
    /**
     * 验证自定义CDN配置
     */
    private fun validateCustomCdnConfig(config: Map<String, Any>) {
        // 自定义CDN可以有灵活的配置参数
        // 这里只做基本验证
        val headers = config["headers"] as? Map<*, *>
        val cacheRules = config["cacheRules"] as? List<*>
        
        // 验证自定义头部
        headers?.let { h ->
            h.forEach { (key, value) ->
                require(key is String && key.isNotBlank()) { "HTTP头部名称必须为非空字符串" }
                require(value is String) { "HTTP头部值必须为字符串" }
            }
        }
        
        // 验证缓存规则
        cacheRules?.let { rules ->
            rules.forEach { rule ->
                require(rule is Map<*, *>) { "缓存规则必须为对象格式" }
                val ruleMap = rule as Map<*, *>
                require(ruleMap["pattern"] is String) { "缓存规则必须包含pattern字段" }
                require(ruleMap["ttl"] is Number) { "缓存规则必须包含ttl字段" }
            }
        }
    }
    
    /**
     * 验证缓存时间设置
     */
    private fun validateCacheTtl(ttl: Any?): Boolean {
        return when (ttl) {
            is Number -> ttl.toLong() >= 0
            is String -> ttl.toLongOrNull()?.let { it >= 0 } ?: false
            else -> false
        }
    }
    
    /**
     * 获取CDN配置摘要
     */
    private fun getConfigSummary(provider: CdnProvider, domain: String): String {
        return "${provider.displayName}: $domain"
    }
} 