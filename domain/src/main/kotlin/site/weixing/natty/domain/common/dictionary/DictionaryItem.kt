package site.weixing.natty.domain.common.dictionary

import site.weixing.natty.api.common.dictionary.DictionaryItemStatus

/**
 * 字典项实体
 */
data class DictionaryItem(
    val itemCode: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null,
    val status: DictionaryItemStatus = DictionaryItemStatus.ACTIVE
)