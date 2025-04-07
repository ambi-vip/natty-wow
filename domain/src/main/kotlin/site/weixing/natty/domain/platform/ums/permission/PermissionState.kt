package site.weixing.natty.domain.platform.ums.permission

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.platform.api.ums.PermissionCreated
import site.weixing.natty.platform.api.ums.PermissionType
import site.weixing.natty.platform.api.ums.PermissionUpdated

class PermissionState(override val id: String) : Identifier {
    var name: String? = null
        private set
        
    var code: String? = null
        private set
        
    var description: String? = null
        private set
        
    var parentId: String? = null
        private set
        
    var type: PermissionType = PermissionType.MENU
        private set

    @OnSourcing
    fun onCreated(event: PermissionCreated) {
        name = event.name
        code = event.code
        description = event.description
        parentId = event.parentId
        type = event.type
    }

    @OnSourcing
    fun onUpdated(event: PermissionUpdated) {
        event.name?.let { name = it }
        event.code?.let { code = it }
        event.description?.let { description = it }
        event.parentId?.let { parentId = it }
        event.type?.let { type = it }
    }
} 