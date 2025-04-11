package site.weixing.natty.platform.api.ums

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
    val name: String,

    @field:NotBlank
    val code: String,

    val description: String? = null,

    val parentId: String? = null,

    val type: PermissionType = PermissionType.MENU
)

@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新权限"
)
data class UpdatePermission(
    @CommandRoute.PathVariable
    val id: String,

    val name: String? = null,

    val code: String? = null,

    val description: String? = null,

    val parentId: String? = null,

    val type: PermissionType? = null
)

enum class PermissionType {
    MENU,
    BUTTON,
    API
}

data class PermissionCreated(
    val name: String,
    val code: String,
    val description: String?,
    val parentId: String?,
    val type: PermissionType
)

data class PermissionUpdated(
    val name: String?,
    val code: String?,
    val description: String?,
    val parentId: String?,
    val type: PermissionType?
)
