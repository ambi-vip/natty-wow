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
    val actualStoragePath: String, // 实际物理存储路径
    val checksum: String,
    val isPublic: Boolean,
    val tags: List<String>,
    val customMetadata: Map<String, String>,
    val storageProviderId: String, // 使用的存储提供商配置id
    val storageProvider: String, // 使用的存储提供商
    val uploadTimestamp: Long // 上传时间戳
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