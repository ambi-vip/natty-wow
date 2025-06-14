package site.weixing.natty.api.common.dictionary

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import jakarta.validation.constraints.NotBlank

/**
 * 创建字典命令
 *
 * @param code 字典编码，唯一
 * @param name 字典名称
 * @param description 字典描述
 */
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建字典"
)
data class CreateDictionary(
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val name: String,
    val description: String? = null
)

/**
 * 字典创建事件
 *
 * @param dictionaryId 字典ID
 * @param code 字典编码
 * @param name 字典名称
 * @param description 字典描述
 */
data class DictionaryCreated(
    val dictionaryId: String,
    val code: String,
    val name: String,
    val description: String? = null
)