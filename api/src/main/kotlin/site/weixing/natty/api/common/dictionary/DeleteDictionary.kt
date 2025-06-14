package site.weixing.natty.api.common.dictionary

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.command.DeleteAggregate

/**
 * 删除字典命令 (逻辑删除)
 *
 * @param id 字典ID
 */
@CommandRoute(
    summary = "删除字典"
)
data class DeleteDictionary(@CommandRoute.PathVariable val id: String) : DeleteAggregate

/**
 * 字典删除事件 (逻辑删除)
 *
 * @param dictionaryId 字典ID
 */
data class DictionaryDeleted(
    val dictionaryId: String
)