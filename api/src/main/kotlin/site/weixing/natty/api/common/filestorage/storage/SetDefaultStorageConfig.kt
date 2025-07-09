package site.weixing.natty.api.common.filestorage.storage

import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "set-default",
    summary = "设置为默认存储配置"
)
data class SetDefaultStorageConfig(
    val enabled: Boolean = true // 是否启用为默认配置
) 