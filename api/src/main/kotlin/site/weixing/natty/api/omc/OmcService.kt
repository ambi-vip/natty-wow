package site.weixing.natty.api.omc

import me.ahoo.wow.api.annotation.BoundedContext

@BoundedContext(
    name = OmcService.SERVICE_NAME,
    alias = OmcService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(OmcService.DEPARTMENT_AGGREGATE_NAME)
    ],
)
object OmcService {
    const val SERVICE_NAME = "omc-service"
    const val SERVICE_ALIAS = "omc"
    const val DEPARTMENT_AGGREGATE_NAME = "department"
}
