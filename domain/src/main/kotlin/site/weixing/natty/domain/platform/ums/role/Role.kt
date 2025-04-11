package site.weixing.natty.domain.platform.ums.role

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import site.weixing.natty.platform.api.ums.CreateRole
import site.weixing.natty.platform.api.ums.RoleCreated
import site.weixing.natty.platform.api.ums.RoleUpdated
import site.weixing.natty.platform.api.ums.UpdateRole

@Suppress("unused")
@AggregateRoot
@StaticTenantId
class Role(private val state: RoleState) {

    @OnCommand
    fun onCreate(command: CreateRole): RoleCreated {
        return RoleCreated(
            name = command.name,
            code = command.code,
            description = command.description,
            permissionIds = command.permissionIds
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateRole): RoleUpdated {
        return RoleUpdated(
            name = command.name,
            code = command.code,
            description = command.description,
            permissionIds = command.permissionIds
        )
    }
}
