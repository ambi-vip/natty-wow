package site.weixing.natty.auth.authorization

import me.ahoo.cosec.api.permission.AppPermission
import me.ahoo.cosec.api.permission.AppRolePermission
import me.ahoo.cosec.authorization.AppRolePermissionRepository
import me.ahoo.cosec.permission.AppRolePermissionData
import me.ahoo.cosec.permission.RolePermissionData
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap

/**
 * InMemoryAppRolePermissionRepository
 * @author ambi
 */
@Component
class InMemoryAppRolePermissionRepository : AppRolePermissionRepository {
    private val appPermissions = ConcurrentHashMap<String, AppPermission>()
    private val rolePermissions = ConcurrentHashMap<String, Set<String>>()

    override fun getAppRolePermission(appId: String, roleIds: Set<String>): Mono<AppRolePermission> {
        val appPermission = appPermissions[appId] ?: return Mono.empty()

        val rolePermissionList = roleIds.mapNotNull { roleId ->
            val permissions = rolePermissions[roleId] ?: return@mapNotNull null
            RolePermissionData(roleId, permissions)
        }

        return AppRolePermissionData(appPermission, rolePermissionList).toMono()
    }

    // 添加设置权限的方法
    fun setAppPermission(appPermission: AppPermission) {
        appPermissions[appPermission.id] = appPermission
    }

    fun setRolePermissions(roleId: String, permissions: Set<String>) {
        rolePermissions[roleId] = permissions
    }
}