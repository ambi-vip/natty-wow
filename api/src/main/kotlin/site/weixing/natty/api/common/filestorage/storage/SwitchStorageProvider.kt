package site.weixing.natty.api.common.filestorage.storage

import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotNull

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "switch-provider",
    summary = "切换存储提供商"
)
data class SwitchStorageProvider(
    @field:NotNull(message = "新存储提供商不能为空")
    val newProvider: StorageProvider,
    
    @field:NotNull(message = "新配置参数不能为空")
    val newConfig: Map<String, Any>
) 