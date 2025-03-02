package site.weixing.natty

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.DemoService.DEMO_AGGREGATE_NAME
import site.weixing.natty.DemoService.VOLUNTEER_PROJECT_AGGREGATE_NAME
import site.weixing.natty.api.demo.CreateDemo
import site.weixing.natty.api.volunteer.CreateVolunteerProject

@BoundedContext(
    DemoService.SERVICE_NAME,
    DemoService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(DEMO_AGGREGATE_NAME, packageScopes = [CreateDemo::class]),
        BoundedContext.Aggregate(VOLUNTEER_PROJECT_AGGREGATE_NAME, packageScopes = [CreateVolunteerProject::class]),
    ],
)
object DemoService {
    const val SERVICE_NAME = "demo-service"
    const val SERVICE_ALIAS = "demo Test"
    const val DEMO_AGGREGATE_NAME = "demo"
    const val VOLUNTEER_PROJECT_AGGREGATE_NAME = "volunteer_project"
}
