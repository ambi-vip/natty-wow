package site.weixing.natty.server.common.filestorage

/**
 * 文件上传相关异常
 */
sealed class FileUploadException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 文件上传业务异常
 */
class FileUploadBusinessException(
    message: String,
    val errorCode: String,
    cause: Throwable? = null
) : FileUploadException(message, cause)

/**
 * 文件上传技术异常
 */
class FileUploadTechnicalException(
    message: String,
    cause: Throwable? = null
) : FileUploadException(message, cause)

/**
 * 文件验证异常
 */
class FileValidationException(
    message: String,
    val field: String,
    cause: Throwable? = null
) : FileUploadException(message, cause)

/**
 * 临时文件处理异常
 */
class TemporaryFileException(
    message: String,
    val tempFileId: String? = null,
    cause: Throwable? = null
) : FileUploadException(message, cause)