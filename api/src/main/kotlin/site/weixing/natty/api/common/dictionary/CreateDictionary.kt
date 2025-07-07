package site.weixing.natty.api.common.dictionary

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

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
