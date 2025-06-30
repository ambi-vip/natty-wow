package site.weixing.natty.api.common.dictionary.item

import me.ahoo.wow.api.event.AggregateDeleted

/**
 * 字典项删除事件 (逻辑删除)
 *
 * @param dictionaryItemId 字典项ID
 * @param dictionaryCode 所属字典的Code
 */
data class DictionaryItemDeleted(
    val dictionaryItemId: String,
    val dictionaryCode: String
) : AggregateDeleted
