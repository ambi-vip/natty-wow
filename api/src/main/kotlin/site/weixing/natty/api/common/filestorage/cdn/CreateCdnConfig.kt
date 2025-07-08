package site.weixing.natty.api.common.filestorage.cdn

import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建CDN配置"
)
data class CreateCdnConfig(
    @field:NotBlank(message = "配置名称不能为空")
    val name: String,
    
    @field:NotBlank(message = "CDN域名不能为空")
    val domain: String,
    
    @field:NotNull(message = "CDN提供商不能为空")
    val provider: CdnProvider,
    
    @field:NotNull(message = "配置参数不能为空")
    val config: Map<String, Any>,
    
    val isDefault: Boolean = false,
    
    val isEnabled: Boolean = true,
    
    val description: String? = null
) 