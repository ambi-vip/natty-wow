package site.weixing.natty.api.common.dictionary.item

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

/**
 * 创建字典项命令
 *
 * @param dictionaryId 所属字典的ID
 * @param itemCode 字典项编码，在同一个字典下唯一
 * @param itemName 字典项名称
 * @param itemValue 字典项值
 * @param sortOrder 排序
 * @param description 字典项描述
 * @param localizedNames 多语言名称，可选
 */
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建字典项"
)
data class CreateDictionaryItem(
    @field:NotBlank
    val dictionaryId: String,
    @field:NotBlank
    val itemCode: String,
    @field:NotBlank
    val itemName: String,

    val itemValue: String?,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 字典项创建事件
 *
 * @param dictionaryItemId 字典项ID
 * @param dictionaryId 所属字典的ID
 * @param dictionaryCode 所属字典的Code
 * @param itemCode 字典项编码
 * @param itemName 字典项名称
 * @param itemValue 字典项值
 * @param sortOrder 排序
 * @param description 字典项描述
 * @param localizedNames 多语言名称，可选
 */
data class DictionaryItemCreated(
    val dictionaryItemId: String,
    val dictionaryId: String,
    val dictionaryCode: String,
    val itemCode: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)
