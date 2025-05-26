package site.weixing.natty.ums.domain.role

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.ums.api.role.CreateRole
import site.weixing.natty.ums.api.role.DeleteRole
import site.weixing.natty.ums.api.role.RoleCreated
import site.weixing.natty.ums.api.role.RoleDeleted
import site.weixing.natty.ums.api.role.RoleUpdated
import site.weixing.natty.ums.api.role.UpdateRole

@Suppress("unused")
@AggregateRoot
class Role(private val state: RoleState) {

    @OnCommand
    fun onCreate(command: CreateRole): RoleCreated {
        return RoleCreated(
            name = command.name,
            description = command.description,
            permissions = command.permissions
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateRole): RoleUpdated {
        if (state.status == RoleStatus.DISABLED) {
            throw IllegalStateException("Cannot update disabled role")
        }
        return RoleUpdated(
            roleId = command.id,
            name = command.name,
            description = command.description,
            permissions = command.permissions
        )
    }

    @OnCommand
    fun onDelete(command: DeleteRole): RoleDeleted {
        if (state.status == RoleStatus.DISABLED) {
            throw IllegalStateException("Role is already disabled")
        }
        return RoleDeleted(
            roleId = command.id
        )
    }
}
