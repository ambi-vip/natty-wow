package site.weixing.natty.api.common.dictionary.item

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

/**
 * 更新字典项命令
 *
 * @param id 字典项ID
 * @param itemName 字典项名称
 * @param itemValue 字典项值
 * @param sortOrder 排序
 * @param description 字典项描述
 * @param localizedNames 多语言名称，可选
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新字典项"
)
data class UpdateDictionaryItem(
    @CommandRoute.PathVariable
    val id: String,
    @field:NotBlank
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 字典项更新事件
 *
 * @param dictionaryItemId 字典项ID
 * @param itemName 字典项名称
 * @param itemValue 字典项值
 * @param sortOrder 排序
 * @param description 字典项描述
 * @param localizedNames 多语言名称，可选
 */
data class DictionaryItemUpdated(
    val dictionaryItemId: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)
