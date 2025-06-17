package site.weixing.natty.api.common

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.api.common.dictionary.CreateDictionary
import site.weixing.natty.api.common.dictionary.item.CreateDictionaryItem

@BoundedContext(
    name = CommonService.SERVICE_NAME,
    alias = CommonService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(CommonService.DICT_AGGREGATE_NAME, packageScopes = [CreateDictionary::class]),
        BoundedContext.Aggregate(CommonService.DICT_ITEM_AGGREGATE_NAME, packageScopes = [CreateDictionaryItem::class]),
    ],
)
object CommonService {
    const val SERVICE_NAME = "common-service"
    const val SERVICE_ALIAS = "common"
    const val DICT_AGGREGATE_NAME = "dict"
    const val DICT_ITEM_AGGREGATE_NAME = "dict_item"
}
