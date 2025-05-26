package site.weixing.natty.ums.api.permission

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建权限"
)
data class CreatePermission(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val name: String,

    val description: String? = null,

    val type: PermissionType = PermissionType.MENU
)

data class PermissionCreated(
    val code: String,
    val name: String,
    val description: String?,
    val type: PermissionType
)

enum class PermissionType {
    MENU,
    OPERATION,
    DATA
}
