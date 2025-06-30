package site.weixing.natty.api.common.dictionary.item

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

/**
 * 改变字典项状态命令
 *
 * @param id 字典项ID
 * @param status 字典项状态
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "status",
    summary = "改变字典项状态"
)
data class ChangeDictionaryItemStatus(
    @CommandRoute.PathVariable
    val id: String,
    @field:NotBlank
    val status: String
)

/**
 * 字典项状态改变事件
 *
 * @param dictionaryItemId 字典项ID
 * @param status 字典项状态
 */
data class DictionaryItemStatusChanged(
    val dictionaryItemId: String,
    val status: String
)
