package site.weixing.natty.api.ums.user

import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "custom-data",
    summary = "更新用户自定义数据"
)
data class UpdateUserCustomData(
    @field:NotNull
    val customData: Map<String, Any>
)

data class UserCustomDataUpdated(
    val customData: Map<String, Any>,
    val updatedAt: Long = System.currentTimeMillis()
) 