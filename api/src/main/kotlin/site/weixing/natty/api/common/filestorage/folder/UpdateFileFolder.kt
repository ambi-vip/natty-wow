package site.weixing.natty.api.common.filestorage.folder

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.command.Update
import me.ahoo.wow.models.tree.command.Updated
import jakarta.validation.constraints.Size
import me.ahoo.wow.models.tree.Flat

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新文件夹"
)
data class UpdateFileFolder(
    @field:Size(min = 1, max = 255, message = "文件夹名称长度必须在1-255字符之间")
    val name: String,
    override val code: String,

    val description: String? = null,

    val permissions: Map<String, Set<String>>? = null,

    val metadata: Map<String, String>? = null
) : Update<FileFolderUpdated> {


    override fun toEvent(previous: Flat): FileFolderUpdated {
        return FileFolderUpdated(
            name = name,
            code,
            previous.sortId,
            description = description,
            permissions = permissions,
            metadata = metadata
        )
    }
}

data class FileFolderUpdated(
    override val name: String,
    override val code: String,
    override val sortId: Int,
    val description: String?,
    val permissions: Map<String, Set<String>>?,
    val metadata: Map<String, String>?
) : Updated 