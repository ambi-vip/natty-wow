package site.weixing.natty.api.common.filestorage.storage

import me.ahoo.wow.api.event.AggregateDeleted

/**
 * 存储配置创建事件
 */
data class StorageConfigCreated(
    val name: String,
    val provider: StorageProvider,
    val config: Map<String, Any>,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val description: String?
)

/**
 * 存储配置更新事件
 */
data class StorageConfigUpdated(
    val name: String?,
    val config: Map<String, Any>?,
    val isEnabled: Boolean?,
    val description: String?
)

/**
 * 存储提供商切换事件
 */
data class StorageProviderSwitched(
    val oldProvider: StorageProvider,
    val newProvider: StorageProvider,
    val newConfig: Map<String, Any>
)

/**
 * 存储配置删除事件
 */
data class StorageConfigDeleted(
    val name: String,
    val provider: StorageProvider
) : AggregateDeleted

/**
 * 默认存储配置变更事件
 */
data class DefaultStorageConfigChanged(
    val oldConfigId: String?,
    val newConfigId: String
) 