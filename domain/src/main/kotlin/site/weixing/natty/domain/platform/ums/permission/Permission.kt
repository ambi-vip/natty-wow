package site.weixing.natty.domain.platform.ums.permission

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import site.weixing.natty.platform.api.ums.CreatePermission
import site.weixing.natty.platform.api.ums.PermissionCreated
import site.weixing.natty.platform.api.ums.PermissionUpdated
import site.weixing.natty.platform.api.ums.UpdatePermission

@Suppress("unused")
@AggregateRoot
@StaticTenantId
class Permission(private val state: PermissionState) {

    @OnCommand
    fun onCreate(command: CreatePermission): PermissionCreated {
        return PermissionCreated(
            name = command.name,
            code = command.code,
            description = command.description,
            parentId = command.parentId,
            type = command.type
        )
    }

    @OnCommand
    fun onUpdate(command: UpdatePermission): PermissionUpdated {
        return PermissionUpdated(
            name = command.name,
            code = command.code,
            description = command.description,
            parentId = command.parentId,
            type = command.type
        )
    }
} 