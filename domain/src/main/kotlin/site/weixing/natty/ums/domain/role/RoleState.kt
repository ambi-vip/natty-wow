package site.weixing.natty.ums.domain.role

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.ums.api.role.RoleCreated
import site.weixing.natty.ums.api.role.RoleDeleted
import site.weixing.natty.ums.api.role.RoleUpdated

class RoleState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var description: String? = null
        private set
    var permissions: Set<String> = emptySet()
        private set
    var status: RoleStatus = RoleStatus.ACTIVE
        private set

    @OnSourcing
    fun onCreated(event: RoleCreated) {
        name = event.name
        description = event.description
        permissions = event.permissions
    }

    @OnSourcing
    fun onUpdated(event: RoleUpdated) {
        name = event.name
        description = event.description
        permissions = event.permissions
    }

    @OnSourcing
    fun onDeleted(event: RoleDeleted) {
        status = RoleStatus.DISABLED
    }
}

enum class RoleStatus {
    ACTIVE,
    DISABLED
}
