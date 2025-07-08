package site.weixing.natty.api.common.filestorage.folder

import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.command.Move
import me.ahoo.wow.models.tree.command.Moved
import jakarta.validation.constraints.NotBlank

@CommandRoute(
    method = CommandRoute.Method.PATCH,
    action = "/move",
    summary = "移动文件夹"
)
data class MoveFileFolder(
    override val codes: List<String>,
    @field:NotBlank(message = "目标父文件夹编码不能为空")
    val targetParentCode: String,

    val targetSortId: Int? = null
) : Move<FileFolderMoved> {

    override fun toEvent(): FileFolderMoved {
        return FileFolderMoved(
            codes,
            oldParentCode = "",
            newParentCode = "",
            oldSortId = 0,
            newSortId = 0
        )
    }

    fun toEvent(oldParentCode: String, newParentCode: String, oldSortId: Int, newSortId: Int): FileFolderMoved {
        return FileFolderMoved(
            codes,
            oldParentCode = oldParentCode,
            newParentCode = newParentCode,
            oldSortId = oldSortId,
            newSortId = newSortId
        )
    }
}

data class FileFolderMoved(
    override val codes: List<String>,
    val oldParentCode: String,
    val newParentCode: String,
    val oldSortId: Int,
    val newSortId: Int
) : Moved 