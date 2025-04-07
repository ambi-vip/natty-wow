package site.weixing.natty.demo

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.demo.NattyService.DEMO_AGGREGATE_NAME
import site.weixing.natty.demo.api.demo.CreateDemo

@BoundedContext(
    name = NattyService.SERVICE_NAME,
    alias = NattyService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(DEMO_AGGREGATE_NAME, packageScopes = [CreateDemo::class]),
    ],
)
object NattyService {
    const val SERVICE_NAME = "natty-service"
    const val SERVICE_ALIAS = "natty"
    const val DEMO_AGGREGATE_NAME = "demo"
}
