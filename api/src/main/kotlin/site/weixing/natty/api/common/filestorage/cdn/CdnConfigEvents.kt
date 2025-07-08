package site.weixing.natty.api.common.filestorage.cdn

/**
 * CDN配置创建事件
 */
data class CdnConfigCreated(
    val name: String,
    val domain: String,
    val provider: CdnProvider,
    val config: Map<String, Any>,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val description: String?
)

/**
 * CDN配置更新事件
 */
data class CdnConfigUpdated(
    val name: String?,
    val domain: String?,
    val config: Map<String, Any>?,
    val isEnabled: Boolean?,
    val description: String?
)

/**
 * CDN启用事件
 */
data class CdnEnabled(
    val name: String,
    val domain: String
)

/**
 * CDN禁用事件
 */
data class CdnDisabled(
    val name: String,
    val domain: String,
    val reason: String?
)

/**
 * CDN配置删除事件
 */
data class CdnConfigDeleted(
    val name: String,
    val domain: String,
    val provider: CdnProvider
)

/**
 * 默认CDN配置变更事件
 */
data class DefaultCdnConfigChanged(
    val oldConfigId: String?,
    val newConfigId: String,
    val newDomain: String
) 