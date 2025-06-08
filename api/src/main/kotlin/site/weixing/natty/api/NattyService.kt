package site.weixing.natty.api

import me.ahoo.wow.api.annotation.BoundedContext

@BoundedContext(
    name = NattyService.SERVICE_NAME,
    alias = NattyService.SERVICE_ALIAS,
)
object NattyService {
    const val SERVICE_NAME = "natty-service"
    const val SERVICE_ALIAS = "natty"
}
