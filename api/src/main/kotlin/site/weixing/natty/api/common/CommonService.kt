package site.weixing.natty.api.common

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.api.common.dictionary.CreateDictionary

@BoundedContext(
    name = CommonService.SERVICE_NAME,
    alias = CommonService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(CommonService.DICT_AGGREGATE_NAME, packageScopes = [CreateDictionary::class]),
    ],
)
object CommonService {
    const val SERVICE_NAME = "common-service"
    const val SERVICE_ALIAS = "common"
    const val DICT_AGGREGATE_NAME = "dict"
}
