package site.weixing.natty.api.auth

import me.ahoo.wow.api.annotation.BoundedContext

@BoundedContext(
    name = AuthService.SERVICE_NAME,
    alias = AuthService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(AuthService.AUTHENTICATE_AGGREGATE_NAME, packageScopes = [Authenticate::class]),
    ],
)
object AuthService {
    const val SERVICE_NAME = "natty-wow"
    const val SERVICE_ALIAS = "auth"
    const val AUTHENTICATE_AGGREGATE_NAME = "authenticate"
}
