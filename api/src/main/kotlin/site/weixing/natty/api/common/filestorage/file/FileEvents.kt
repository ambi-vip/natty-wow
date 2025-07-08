package site.weixing.natty.api.common.filestorage.file

/**
 * 文件上传事件
 */
data class FileUploaded(
    val fileName: String,
    val folderId: String,
    val uploaderId: String,
    val fileSize: Long,
    val contentType: String,
    val storagePath: String,
    val checksum: String?,
    val isPublic: Boolean,
    val tags: List<String>,
    val customMetadata: Map<String, String>,
    // 临时解决方案：在事件中包含文件内容引用ID
    // 在生产环境中，应该使用临时存储服务
    val tempFileId: String
)

/**
 * 文件更新事件
 */
data class FileUpdated(
    val fileName: String?,
    val isPublic: Boolean?,
    val tags: List<String>?,
    val customMetadata: Map<String, String>?
)

/**
 * 文件删除事件
 */
data class FileDeleted(
    val fileName: String,
    val storagePath: String,
    val reason: String?
)

/**
 * 文件移动事件
 */
data class FileMoved(
    val fileName: String,
    val oldFolderId: String,
    val newFolderId: String,
    val oldStoragePath: String,
    val newStoragePath: String
)

/**
 * 文件复制事件
 */
data class FileCopied(
    val sourceFileName: String,
    val targetFileName: String,
    val sourceFolderId: String,
    val targetFolderId: String,
    val sourceStoragePath: String,
    val targetStoragePath: String
)

/**
 * 文件状态变更事件
 */
data class FileStatusChanged(
    val fileName: String,
    val oldStatus: FileStatus,
    val newStatus: FileStatus,
    val reason: String?
) 