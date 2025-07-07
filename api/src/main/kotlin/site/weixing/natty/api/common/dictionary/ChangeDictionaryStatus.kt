package site.weixing.natty.api.common.dictionary

import me.ahoo.wow.api.annotation.CommandRoute

/**
 * 改变字典状态命令
 *
 * @param id 字典ID
 * @param status 字典状态
 */
@CommandRoute(
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "status",
    summary = "改变字典状态"
)
data class ChangeDictionaryStatus(
    @CommandRoute.PathVariable
    val id: String,
    val status: DictionaryStatus
)
