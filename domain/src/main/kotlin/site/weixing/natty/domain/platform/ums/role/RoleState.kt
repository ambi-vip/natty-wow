package site.weixing.natty.domain.platform.ums.role

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.platform.api.ums.RoleCreated
import site.weixing.natty.platform.api.ums.RoleUpdated

class RoleState(override val id: String) : Identifier {
    var name: String? = null
        private set

    var code: String? = null
        private set

    var description: String? = null
        private set

    var permissionIds: List<String> = emptyList()
        private set

    @OnSourcing
    fun onCreated(event: RoleCreated) {
        name = event.name
        code = event.code
        description = event.description
        permissionIds = event.permissionIds
    }

    @OnSourcing
    fun onUpdated(event: RoleUpdated) {
        event.name?.let { name = it }
        event.code?.let { code = it }
        event.description?.let { description = it }
        event.permissionIds?.let { permissionIds = it }
    }
}
