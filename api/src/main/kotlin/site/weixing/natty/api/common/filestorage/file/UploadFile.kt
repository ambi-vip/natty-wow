package site.weixing.natty.api.common.filestorage.file

import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "/upload",
    summary = "上传文件"
)
data class UploadFile(
    @field:NotBlank(message = "文件名不能为空")
    val fileName: String,
    
    @field:NotBlank(message = "文件夹ID不能为空")
    val folderId: String,
    
    @field:NotBlank(message = "上传者ID不能为空")
    val uploaderId: String,
    
    @field:Min(value = 1, message = "文件大小必须大于0")
    val fileSize: Long,
    
    @field:NotBlank(message = "内容类型不能为空")
    val contentType: String,
    
    @field:NotBlank(message = "临时文件引用不能为空")
    val temporaryFileReference: String,
    
    val checksum: String? = null,
    
    val isPublic: Boolean = false,
    
    val tags: List<String> = emptyList(),
    
    val customMetadata: Map<String, String> = emptyMap(),
    
    val replaceIfExists: Boolean = false
)