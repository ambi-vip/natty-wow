package site.weixing.natty.api.common.filestorage.file

import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.annotation.CommandRoute
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import me.ahoo.wow.api.command.validation.CommandValidator
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions

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
    val content: Flux<DataBuffer>,

    val isPublic: Boolean = false,

    val tags: List<String> = emptyList(),

    val customMetadata: Map<String, String> = emptyMap(),

    val processingOptions: ProcessingOptions = ProcessingOptions(),

    val checksum: String? = null
) : CommandValidator {

    override fun validate() {
        validateFileName(fileName)
        validateFileSize(fileSize)
        validateContentType(contentType)
        validateTags(tags)
    }

    /**
     * 验证文件名是否合法
     */
    private fun validateFileName(fileName: String) {
        // 文件名不能包含特殊字符
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        require(fileName.none { it in invalidChars }) {
            "文件名包含非法字符: ${invalidChars.joinToString("")}"
        }

        // 文件名长度限制
        require(fileName.length <= 255) { "文件名长度不能超过255个字符" }

        // 文件名不能以点开头或结尾
        require(!fileName.startsWith(".") && !fileName.endsWith(".")) {
            "文件名不能以点开头或结尾"
        }

        // 文件名不能为空或只包含空白字符
        require(fileName.trim().isNotEmpty()) { "文件名不能为空" }
    }

    /**
     * 验证文件大小
     */
    private fun validateFileSize(fileSize: Long) {
        require(fileSize > 0) { "文件大小必须大于0" }
        
        // 文件大小上限：500MB
        val maxFileSize = 500 * 1024 * 1024L
        require(fileSize <= maxFileSize) { 
            "文件大小超过限制: ${formatFileSize(fileSize)} > ${formatFileSize(maxFileSize)}" 
        }
    }

    /**
     * 验证内容类型
     */
    private fun validateContentType(contentType: String) {
        require(contentType.isNotBlank()) { "文件类型不能为空" }
        
        // 基本的MIME类型格式验证
        require(contentType.contains("/")) { "文件类型格式无效" }
        
        // 禁止的文件类型
        val forbiddenTypes = setOf(
            "application/x-executable",
            "application/x-msdownload",
            "application/x-msdos-program"
        )
        require(!forbiddenTypes.contains(contentType)) { 
            "不支持的文件类型: $contentType" 
        }
    }

    /**
     * 验证标签
     */
    private fun validateTags(tags: List<String>) {
        // 标签数量限制
        require(tags.size <= 20) { "标签数量不能超过20个" }
        
        // 每个标签长度限制
        tags.forEach { tag ->
            require(tag.length <= 50) { "标签长度不能超过50个字符: $tag" }
            require(tag.trim().isNotEmpty()) { "标签不能为空" }
        }
        
        // 检查重复标签
        require(tags.distinct().size == tags.size) { "存在重复的标签" }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * 检查是否需要处理
     */
    fun requiresProcessing(): Boolean {
        return processingOptions.requiresProcessing()
    }
}