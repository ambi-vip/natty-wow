package site.weixing.natty.api.demo

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.api.demo.DemoService.DEMO_AGGREGATE_NAME
import site.weixing.natty.api.demo.demo.CreateDemo

@BoundedContext(
    name = DemoService.SERVICE_NAME,
    alias = DemoService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(DEMO_AGGREGATE_NAME, packageScopes = [CreateDemo::class]),
    ],
)
object DemoService {
    const val SERVICE_NAME = "natty-service"
    const val SERVICE_ALIAS = "natty"
    const val DEMO_AGGREGATE_NAME = "demo"
}
