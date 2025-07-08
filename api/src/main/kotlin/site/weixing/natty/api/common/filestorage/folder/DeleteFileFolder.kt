package site.weixing.natty.api.common.filestorage.folder

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.Flat
import me.ahoo.wow.models.tree.command.Delete
import me.ahoo.wow.models.tree.command.Deleted

@CommandRoute(
    method = CommandRoute.Method.DELETE,
    action = "",
    summary = "删除文件夹"
)
data class DeleteFileFolder(
    @field:NotBlank
    @CommandRoute.PathVariable
    override val code: String,
    val force: Boolean = false,
    val reason: String? = null
) : Delete<FileFolderDeleted> {

    override fun toEvent(previous: Flat): FileFolderDeleted {
        return FileFolderDeleted(
            code = code,
            force = force,
            reason = reason
        )
    }
}

data class FileFolderDeleted(
    override val code: String,
    val force: Boolean,
    val reason: String?
) : Deleted 