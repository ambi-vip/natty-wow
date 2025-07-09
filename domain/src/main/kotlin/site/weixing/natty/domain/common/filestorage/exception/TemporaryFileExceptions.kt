package site.weixing.natty.domain.common.filestorage.exception

/**
 * 文件存储基础异常类
 */
open class FileStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 临时文件不存在异常
 * 
 * 当尝试访问一个不存在的临时文件时抛出此异常。
 * 可能的原因：
 * 1. 引用ID不正确或已被删除
 * 2. 文件被外部程序删除
 * 3. 系统重启导致临时文件丢失
 * 
 * @param reference 临时文件引用ID
 */
class TemporaryFileNotFoundException(reference: String) : FileStorageException("临时文件不存在: $reference")

/**
 * 临时文件已过期异常
 * 
 * 当尝试访问一个已过期的临时文件时抛出此异常。
 * 临时文件有固定的生命周期，超过有效期后会被自动清理。
 * 
 * @param reference 临时文件引用ID
 */
class TemporaryFileExpiredException(reference: String) : FileStorageException("临时文件已过期: $reference")

/**
 * 临时文件创建失败异常
 * 
 * 当创建临时文件失败时抛出此异常。
 * 可能的原因：
 * 1. 磁盘空间不足
 * 2. 权限不足
 * 3. I/O错误
 * 4. 文件大小超过限制
 * 
 * @param message 错误详细信息
 * @param cause 底层异常原因
 */
class TemporaryFileCreationException(message: String = "临时文件创建失败", cause: Throwable? = null) : FileStorageException(message, cause)

/**
 * 临时文件访问异常
 * 
 * 当访问临时文件时发生I/O错误时抛出此异常。
 * 可能的原因：
 * 1. 文件被锁定
 * 2. 读取权限不足
 * 3. 磁盘I/O错误
 * 4. 网络存储不可用
 * 
 * @param reference 临时文件引用ID
 * @param message 自定义错误信息
 * @param cause 底层异常原因
 */
class TemporaryFileAccessException(reference: String, message: String? = null, cause: Throwable? = null) : 
    FileStorageException(message ?: "临时文件访问失败: $reference", cause)

/**
 * 临时文件大小超限异常
 * 
 * 当临时文件大小超过系统配置的最大限制时抛出此异常。
 * 
 * @param fileSize 实际文件大小
 * @param maxSize 最大允许大小
 */
class TemporaryFileSizeExceededException(fileSize: Long, maxSize: Long) : 
    FileStorageException("临时文件大小超过限制: ${fileSize}字节 > ${maxSize}字节")

/**
 * 临时文件校验失败异常
 * 
 * 当临时文件的校验和不匹配时抛出此异常。
 * 
 * @param reference 临时文件引用ID
 * @param expectedChecksum 期望的校验和
 * @param actualChecksum 实际的校验和
 */
class TemporaryFileChecksumMismatchException(
    reference: String,
    expectedChecksum: String,
    actualChecksum: String
) : FileStorageException("临时文件校验和不匹配: $reference, 期望=$expectedChecksum, 实际=$actualChecksum") 