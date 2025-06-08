package site.weixing.natty.domain.ums.permission

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.ums.api.permission.CreatePermission
import site.weixing.natty.ums.api.permission.PermissionCreated

@Suppress("unused")
@AggregateRoot
class Permission(private val state: PermissionState) {

    @OnCommand
    fun onCreate(command: CreatePermission): PermissionCreated {
        return PermissionCreated(
            code = command.code,
            name = command.name,
            description = command.description,
            type = command.type
        )
    }
}
