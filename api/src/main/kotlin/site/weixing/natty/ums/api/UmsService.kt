package site.weixing.natty.ums.api

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.ums.api.account.CreateAccount
import site.weixing.natty.ums.api.permission.CreatePermission
import site.weixing.natty.ums.api.role.CreateRole

@BoundedContext(
    name = UmsService.SERVICE_NAME,
    alias = UmsService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(UmsService.ACCOUNT_AGGREGATE_NAME, packageScopes = [CreateAccount::class]),
        BoundedContext.Aggregate(UmsService.USER_AGGREGATE_NAME),
        BoundedContext.Aggregate(UmsService.ROLE_AGGREGATE_NAME, packageScopes = [CreateRole::class]),
        BoundedContext.Aggregate(UmsService.PERMISSION_AGGREGATE_NAME, packageScopes = [CreatePermission::class]),
    ],
)
object UmsService {
    const val SERVICE_NAME = "ums-service"
    const val SERVICE_ALIAS = "ums"
    const val ACCOUNT_AGGREGATE_NAME = "account"
    const val USER_AGGREGATE_NAME = "user"
    const val ROLE_AGGREGATE_NAME = "role"
    const val PERMISSION_AGGREGATE_NAME = "permission"
}
