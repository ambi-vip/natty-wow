package site.weixing.natty.api.common.filestorage.storage

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.command.DeleteAggregate

@CommandRoute(
    method = CommandRoute.Method.DELETE,
    action = "",
    summary = "删除存储配置",
    appendIdPath = CommandRoute.AppendPath.ALWAYS
)
data class DeleteStorageConfig(
    val force: Boolean = false // 是否强制删除（即使有文件引用）
)  : DeleteAggregate