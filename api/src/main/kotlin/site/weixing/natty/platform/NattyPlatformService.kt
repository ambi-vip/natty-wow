package site.weixing.natty.platform

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.platform.api.ums.CreateUser
import site.weixing.natty.platform.api.ums.CreateRole
import site.weixing.natty.platform.api.ums.CreatePermission
import site.weixing.natty.platform.NattyPlatformService.PERMISSION_AGGREGATE_NAME
import site.weixing.natty.platform.NattyPlatformService.ROLE_AGGREGATE_NAME
import site.weixing.natty.platform.NattyPlatformService.USER_AGGREGATE_NAME

@BoundedContext(
    name = NattyPlatformService.SERVICE_NAME,
    alias = NattyPlatformService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(USER_AGGREGATE_NAME, packageScopes = [CreateUser::class]),
        BoundedContext.Aggregate(ROLE_AGGREGATE_NAME, packageScopes = [CreateRole::class]),
        BoundedContext.Aggregate(PERMISSION_AGGREGATE_NAME, packageScopes = [CreatePermission::class])
    ],
)
object NattyPlatformService {
    const val SERVICE_NAME = "platform-ums"
    const val SERVICE_ALIAS = "ums"
    const val USER_AGGREGATE_NAME = "user"
    const val ROLE_AGGREGATE_NAME = "role"
    const val PERMISSION_AGGREGATE_NAME = "permission"
}
