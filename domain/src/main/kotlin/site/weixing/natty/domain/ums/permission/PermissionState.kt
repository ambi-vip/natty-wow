package site.weixing.natty.domain.ums.permission

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.ums.api.permission.PermissionCreated
import site.weixing.natty.ums.api.permission.PermissionType
// import site.weixing.natty.ums.api.permission.PermissionDeleted
// import site.weixing.natty.ums.api.permission.PermissionUpdated

class PermissionState(override val id: String) : Identifier {
    var code: String? = null
        private set
    var name: String? = null
        private set
    var description: String? = null
        private set
    var type: PermissionType? = null
        private set
    var status: PermissionStatus = PermissionStatus.ACTIVE
        private set

    @OnSourcing
    fun onCreated(event: PermissionCreated) {
        code = event.code
        name = event.name
        description = event.description
        type = event.type
    }

//    @OnSourcing
//    fun onUpdated(event: PermissionUpdated) {
//        code = event.code
//        name = event.name
//        description = event.description
//        type = event.type
//    }
//
//    @OnSourcing
//    fun onDeleted(event: PermissionDeleted) {
//        status = PermissionStatus.DISABLED
//    }
}

enum class PermissionStatus {
    ACTIVE,
    DISABLED
}
