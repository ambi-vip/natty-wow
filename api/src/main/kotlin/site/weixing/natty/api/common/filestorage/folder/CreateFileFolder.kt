package site.weixing.natty.api.common.filestorage.folder

import me.ahoo.wow.api.annotation.AllowCreate
import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.ahoo.wow.models.tree.command.Create
import me.ahoo.wow.models.tree.command.Created

@AllowCreate
@CommandRoute(
    method = CommandRoute.Method.POST,
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "创建文件夹",
    description = "Id 为租户ID."
)
data class CreateFileFolder(
    @field:NotBlank(message = "文件夹名称不能为空")
    @field:Size(min = 1, max = 255, message = "文件夹名称长度必须在1-255字符之间")
    override val name: String,
    
    @field:NotBlank(message = "父文件夹编码不能为空")
    override val parentCode: String,
    
    val description: String? = null,
    
    val permissions: Map<String, Set<String>> = emptyMap(),
    
    val metadata: Map<String, String> = emptyMap()
) : Create<FileFolderCreated> {

    override fun toEvent(code: String, sortId: Int): FileFolderCreated {
        return FileFolderCreated(
            name = name,
            code = code,
            sortId = sortId,
            description = description,
            permissions = permissions,
            metadata = metadata
        )
    }
}

data class FileFolderCreated(
    override val name: String,
    override val code: String,
    override val sortId: Int,
    val description: String?,
    val permissions: Map<String, Set<String>>,
    val metadata: Map<String, String>
) : Created 