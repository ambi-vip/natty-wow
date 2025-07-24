package site.weixing.natty.api.common.filestorage.file

/**
 * 文件上传响应
 * API契约，定义文件上传操作的响应格式
 */
data class FileUploadResponse(
    val fileId: String?,
    val fileName: String?,
    val fileSize: Long,
    val uploadMethod: String,
    val message: String,
    val checksum: String? = null,
    val storagePath: String? = null,
    val processingRequired: Boolean = false
)