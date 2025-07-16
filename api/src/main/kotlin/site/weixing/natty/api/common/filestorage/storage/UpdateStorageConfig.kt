package site.weixing.natty.api.common.filestorage.storage

import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新存储配置"
)
data class UpdateStorageConfig(
    @field:NotBlank(message = "配置名称不能为空")
    val name: String? = null,
    
    val config: Map<String, Any>? = null,
    
    val isEnabled: Boolean? = null,
    
    val description: String? = null
) 