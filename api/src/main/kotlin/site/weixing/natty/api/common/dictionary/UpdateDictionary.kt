package site.weixing.natty.api.common.dictionary

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute

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
