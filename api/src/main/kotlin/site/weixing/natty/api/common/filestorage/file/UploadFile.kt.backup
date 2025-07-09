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
    
    @field:NotNull(message = "文件内容不能为空")
    val fileContent: ByteArray,
    
    val checksum: String? = null,
    
    val isPublic: Boolean = false,
    
    val tags: List<String> = emptyList(),
    
    val customMetadata: Map<String, String> = emptyMap(),
    
    val replaceIfExists: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadFile

        if (fileName != other.fileName) return false
        if (folderId != other.folderId) return false
        if (uploaderId != other.uploaderId) return false
        if (fileSize != other.fileSize) return false
        if (contentType != other.contentType) return false
        if (!fileContent.contentEquals(other.fileContent)) return false
        if (checksum != other.checksum) return false
        if (isPublic != other.isPublic) return false
        if (tags != other.tags) return false
        if (customMetadata != other.customMetadata) return false
        if (replaceIfExists != other.replaceIfExists) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + uploaderId.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + isPublic.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + customMetadata.hashCode()
        result = 31 * result + replaceIfExists.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "UploadFile(" +
                "fileName='$fileName', " +
                "folderId='$folderId', " +
                "uploaderId='$uploaderId', " +
                "fileSize=$fileSize, " +
                "contentType='$contentType', " +
                "fileContent=[ByteArray(size=${fileContent.size})], " +
                "checksum=$checksum, " +
                "isPublic=$isPublic, " +
                "tags=$tags, " +
                "customMetadata=$customMetadata, " +
                "replaceIfExists=$replaceIfExists" +
                ")"
    }
} 