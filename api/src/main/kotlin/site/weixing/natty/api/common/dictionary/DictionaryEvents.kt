package site.weixing.natty.api.common.dictionary

/**
 * 字典创建事件
 */
data class DictionaryCreated(
    val dictionaryId: String,
    val code: String,
    val name: String,
    val description: String? = null
)

/**
 * 字典更新事件
 */
data class DictionaryUpdated(
    val dictionaryId: String,
    val name: String,
    val description: String? = null
)

/**
 * 字典状态改变事件
 */
data class DictionaryStatusChanged(
    val dictionaryId: String,
    val status: DictionaryStatus
)

/**
 * 字典删除事件
 */
data class DictionaryDeleted(
    val dictionaryId: String,
    val code: String,
    val name: String
)

/**
 * 字典项添加事件
 */
data class DictionaryItemAdded(
    val dictionaryId: String,
    val itemCode: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 字典项更新事件
 */
data class DictionaryItemUpdated(
    val dictionaryId: String,
    val itemCode: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 字典项状态改变事件
 */
data class DictionaryItemStatusChanged(
    val dictionaryId: String,
    val itemCode: String,
    val status: DictionaryItemStatus
)

/**
 * 字典项移除事件
 */
data class DictionaryItemRemoved(
    val dictionaryId: String,
    val itemCode: String
) 