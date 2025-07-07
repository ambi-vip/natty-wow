package site.weixing.natty.api.common.dictionary

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

/**
 * 添加字典项命令
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "items",
    method = CommandRoute.Method.POST,
    summary = "添加字典项"
)
data class AddDictionaryItem(
    @CommandRoute.PathVariable
    val dictionaryId: String,
    @field:NotBlank
    val itemCode: String,
    @field:NotBlank
    val itemName: String,
    val itemValue: String? = null,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 更新字典项命令
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "items/{itemCode}",
    method = CommandRoute.Method.PUT,
    summary = "更新字典项"
)
data class UpdateDictionaryItem(
    @CommandRoute.PathVariable
    val dictionaryId: String,
    @CommandRoute.PathVariable
    val itemCode: String,
    @field:NotBlank
    val itemName: String,
    val itemValue: String? = null,
    val sortOrder: Int = 0,
    val description: String? = null,
    val localizedNames: Map<String, String>? = null
)

/**
 * 改变字典项状态命令
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "items/{itemCode}/status",
    method = CommandRoute.Method.PATCH,
    summary = "改变字典项状态"
)
data class ChangeDictionaryItemStatus(
    @CommandRoute.PathVariable
    val dictionaryId: String,
    @CommandRoute.PathVariable
    val itemCode: String,
    val status: DictionaryItemStatus
)

/**
 * 移除字典项命令
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "items/{itemCode}",
    method = CommandRoute.Method.DELETE,
    summary = "移除字典项"
)
data class RemoveDictionaryItem(
    @CommandRoute.PathVariable
    val dictionaryId: String,
    @CommandRoute.PathVariable
    val itemCode: String
) 