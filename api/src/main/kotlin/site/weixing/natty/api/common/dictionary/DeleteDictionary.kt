package site.weixing.natty.api.common.dictionary



/**
 * 字典删除事件 (逻辑删除)
 *
 * @param dictionaryId 字典ID
 */
data class DictionaryDeleted(
    val dictionaryId: String
)