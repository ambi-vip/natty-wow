package site.weixing.natty.server.common.filestorage

import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import site.weixing.natty.api.common.filestorage.file.ProcessingOptions

/**
 * 文件上传请求
 */
data class FileUploadRequest(
    val fileName: String,
    val folderId: String,
    val uploaderId: String,
    val fileSize: Long,
    val contentType: String,
    val content: Flux<DataBuffer>,
    val isPublic: Boolean = false,
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap(),
    val processingOptions: ProcessingOptions = ProcessingOptions(),
    val checksum: String? = null,
    val replaceIfExists: Boolean = false
) {
    // 向后兼容性别名
    val dataBufferFlux: Flux<DataBuffer> get() = content
}

/**
 * 文件下载请求
 */
data class FileDownloadRequest(
    val fileId: String,
    val userId: String? = null // 用于权限验证
)

/**
 * 文件下载结果
 */
data class FileDownloadResult(
    val fileId: String,
    val downloadUrl: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val expiresAt: Long? = null // 下载链接过期时间
)

/**
 * 文件删除请求
 */
data class FileDeleteRequest(
    val fileId: String,
    val deletedBy: String,
    val reason: String? = null
)