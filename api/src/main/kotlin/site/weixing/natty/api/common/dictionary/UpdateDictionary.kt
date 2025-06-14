package site.weixing.natty.api.common.dictionary

import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank

/**
 * 更新字典命令
 *
 * @param id 字典ID
 * @param name 字典名称
 * @param description 字典描述
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新字典"
)
data class UpdateDictionary(
    @CommandRoute.PathVariable
    val id: String,
    @field:NotBlank
    val name: String,
    val description: String? = null
)

/**
 * 字典更新事件
 *
 * @param dictionaryId 字典ID
 * @param name 字典名称
 * @param description 字典描述
 */
data class DictionaryUpdated(
    val dictionaryId: String,
    val name: String,
    val description: String? = null
)