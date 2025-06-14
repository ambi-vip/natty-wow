package site.weixing.natty.api.common.dictionary.item

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.command.DeleteAggregate

/**
 * 删除字典项命令 (逻辑删除)
 *
 * @param id 字典项ID
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "删除字典项"
)
data class DeleteDictionaryItem(@CommandRoute.PathVariable val id: String) : DeleteAggregate

/**
 * 字典项删除事件 (逻辑删除)
 *
 * @param dictionaryItemId 字典项ID
 * @param dictionaryCode 所属字典的Code
 */
data class DictionaryItemDeleted(
    val dictionaryItemId: String,
    val dictionaryCode: String
)