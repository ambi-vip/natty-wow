package site.weixing.natty.domain.common.filestorage.strategy.impl

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import site.weixing.natty.domain.common.filestorage.exception.*
import site.weixing.natty.domain.common.filestorage.exception.FileNotFoundException as CustomFileNotFoundException
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 本地文件存储策略实现
 * 提供完整的本地文件系统操作功能
 */
class LocalFileStorageStrategy(
    private val baseDirectory: String,
    private val maxFileSize: Long = 100 * 1024 * 1024, // 100MB
    private val allowedContentTypes: Set<String> = emptySet(), // 空集合表示不限制
    private val enableChecksumValidation: Boolean = true,
    private val urlPrefix: String = "file://"
) : FileStorageStrategy {

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileStorageStrategy::class.java)
        private const val CHECKSUM_ALGORITHM = "SHA-256"
        private const val BUFFER_SIZE = 8192
    }

    override val provider: StorageProvider = StorageProvider.LOCAL

    private val basePath: Path = Paths.get(baseDirectory).toAbsolutePath()
    private val readWriteLock = ReentrantReadWriteLock()

    init {
        // 确保基础目录存在
        try {
            Files.createDirectories(basePath)
            logger.info("本地存储初始化完成，基础目录: {}", basePath)
        } catch (e: Exception) {
            throw StorageConfigurationException("baseDirectory", "无法创建基础目录: $baseDirectory", e)
        }
    }

    override fun uploadFile(
        filePath: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long,
        metadata: Map<String, String>
    ): Mono<StorageInfo> {
        return Mono.fromCallable {
            validateFilePath(filePath)
            validateFileSize(fileSize)
            validateContentType(contentType)

            readWriteLock.write {
                val targetPath = resolveFilePath(filePath)
                
                // 检查文件是否已存在
                if (Files.exists(targetPath)) {
                    throw site.weixing.natty.domain.common.filestorage.exception.FileAlreadyExistsException(filePath)
                }

                // 确保父目录存在
                Files.createDirectories(targetPath.parent)

                var tempFile: Path? = null
                try {
                    // 先写入临时文件
                    tempFile = Files.createTempFile(targetPath.parent, "upload_", ".tmp")
                    var actualSize = 0L
                    var checksum: String? = null

                    Files.newOutputStream(tempFile).use { outputStream ->
                        if (enableChecksumValidation) {
                            val digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM)
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                digest.update(buffer, 0, bytesRead)
                                actualSize += bytesRead

                                // 检查文件大小
                                if (actualSize > maxFileSize) {
                                    throw FileSizeExceededException(actualSize, maxFileSize)
                                }
                            }

                            checksum = bytesToHex(digest.digest())
                        } else {
                            inputStream.copyTo(outputStream, BUFFER_SIZE)
                            actualSize = Files.size(tempFile)
                        }
                    }

                    // 验证实际大小
                    if (actualSize != fileSize) {
                        logger.warn("文件上传大小不匹配: 期望 {} 字节，实际 {} 字节", fileSize, actualSize)
                    }

                    // 原子性移动文件
                    Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE)
                    tempFile = null

                    logger.info("文件上传成功: {} (大小: {} 字节)", filePath, actualSize)

                    StorageInfo.local(
                        storagePath = filePath,
                        etag = checksum
                    )

                } catch (e: Exception) {
                    // 清理临时文件
                    tempFile?.let { 
                        try {
                            Files.deleteIfExists(it)
                        } catch (cleanupException: Exception) {
                            logger.warn("清理临时文件失败: {}", it, cleanupException)
                        }
                    }
                    
                    when (e) {
                        is FileStorageException -> throw e
                        is IOException -> throw StorageConnectionException("LOCAL", "文件上传失败", e)
                        else -> throw StorageConnectionException("LOCAL", "文件上传过程中发生未知错误", e)
                    }
                }
            }
        }
    }

    override fun downloadFile(filePath: String): Mono<InputStream> {
        return Mono.fromCallable {
            validateFilePath(filePath)

            readWriteLock.read {
                val targetPath = resolveFilePath(filePath)
                
                if (!Files.exists(targetPath)) {
                    throw CustomFileNotFoundException(filePath)
                }

                if (!Files.isRegularFile(targetPath)) {
                    throw InvalidFilePathException(filePath, "路径不是一个文件")
                }

                try {
                    Files.newInputStream(targetPath)
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "文件下载失败", e)
                }
            }
        }
    }

    override fun deleteFile(filePath: String): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(filePath)

            readWriteLock.write {
                val targetPath = resolveFilePath(filePath)
                
                try {
                    val deleted = Files.deleteIfExists(targetPath)
                    if (deleted) {
                        logger.info("文件删除成功: {}", filePath)
                    } else {
                        logger.warn("文件不存在，无法删除: {}", filePath)
                    }
                    deleted
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "文件删除失败", e)
                }
            }
        }
    }

    override fun existsFile(filePath: String): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(filePath)
            val targetPath = resolveFilePath(filePath)
            Files.exists(targetPath) && Files.isRegularFile(targetPath)
        }
    }

    override fun getFileSize(filePath: String): Mono<Long> {
        return Mono.fromCallable {
            validateFilePath(filePath)

            readWriteLock.read {
                val targetPath = resolveFilePath(filePath)
                
                if (!Files.exists(targetPath)) {
                    throw CustomFileNotFoundException(filePath)
                }

                try {
                    Files.size(targetPath)
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "获取文件大小失败", e)
                }
            }
        }
    }

    override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?): Mono<String> {
        return Mono.fromCallable {
            validateFilePath(filePath)
            val targetPath = resolveFilePath(filePath)
            
            if (!Files.exists(targetPath)) {
                throw CustomFileNotFoundException(filePath)
            }

            // 本地文件系统不支持过期时间
            "$urlPrefix${targetPath.toAbsolutePath()}"
        }
    }

    override fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(sourcePath)
            validateFilePath(destPath)

            readWriteLock.write {
                val sourceFile = resolveFilePath(sourcePath)
                val destFile = resolveFilePath(destPath)
                
                if (!Files.exists(sourceFile)) {
                    throw CustomFileNotFoundException(sourcePath)
                }

                if (Files.exists(destFile)) {
                    throw site.weixing.natty.domain.common.filestorage.exception.FileAlreadyExistsException(destPath)
                }

                try {
                    Files.createDirectories(destFile.parent)
                    Files.copy(sourceFile, destFile, StandardCopyOption.COPY_ATTRIBUTES)
                    logger.info("文件复制成功: {} -> {}", sourcePath, destPath)
                    true
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "文件复制失败", e)
                }
            }
        }
    }

    override fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(sourcePath)
            validateFilePath(destPath)

            readWriteLock.write {
                val sourceFile = resolveFilePath(sourcePath)
                val destFile = resolveFilePath(destPath)
                
                if (!Files.exists(sourceFile)) {
                    throw CustomFileNotFoundException(sourcePath)
                }

                if (Files.exists(destFile)) {
                    throw site.weixing.natty.domain.common.filestorage.exception.FileAlreadyExistsException(destPath)
                }

                try {
                    Files.createDirectories(destFile.parent)
                    Files.move(sourceFile, destFile, StandardCopyOption.ATOMIC_MOVE)
                    logger.info("文件移动成功: {} -> {}", sourcePath, destPath)
                    true
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "文件移动失败", e)
                }
            }
        }
    }

    override fun listFiles(directoryPath: String, recursive: Boolean): Mono<List<FileInfo>> {
        return Mono.fromCallable {
            validateFilePath(directoryPath)

            readWriteLock.read {
                val dirPath = resolveFilePath(directoryPath)
                
                if (!Files.exists(dirPath)) {
                    throw CustomFileNotFoundException(directoryPath)
                }

                if (!Files.isDirectory(dirPath)) {
                    throw InvalidFilePathException(directoryPath, "路径不是一个目录")
                }

                try {
                    val fileList = mutableListOf<FileInfo>()
                    
                    if (recursive) {
                        Files.walkFileTree(dirPath, object : SimpleFileVisitor<Path>() {
                            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                                fileList.add(createFileInfo(file, attrs))
                                return FileVisitResult.CONTINUE
                            }

                            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                                if (dir != dirPath) {
                                    fileList.add(createFileInfo(dir, attrs))
                                }
                                return FileVisitResult.CONTINUE
                            }
                        })
                    } else {
                        Files.newDirectoryStream(dirPath).use { stream ->
                            for (path in stream) {
                                val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
                                fileList.add(createFileInfo(path, attrs))
                            }
                        }
                    }

                    fileList.sortedBy { it.name }
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "列出文件失败", e)
                }
            }
        }
    }

    override fun createDirectory(directoryPath: String): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(directoryPath)

            readWriteLock.write {
                val dirPath = resolveFilePath(directoryPath)
                
                try {
                    Files.createDirectories(dirPath)
                    logger.info("目录创建成功: {}", directoryPath)
                    true
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "目录创建失败", e)
                }
            }
        }
    }

    override fun deleteDirectory(directoryPath: String, recursive: Boolean): Mono<Boolean> {
        return Mono.fromCallable {
            validateFilePath(directoryPath)

            readWriteLock.write {
                val dirPath = resolveFilePath(directoryPath)
                
                if (!Files.exists(dirPath)) {
                    return@fromCallable false
                }

                if (!Files.isDirectory(dirPath)) {
                    throw InvalidFilePathException(directoryPath, "路径不是一个目录")
                }

                try {
                    if (recursive) {
                        Files.walkFileTree(dirPath, object : SimpleFileVisitor<Path>() {
                            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                                Files.delete(file)
                                return FileVisitResult.CONTINUE
                            }

                            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                                Files.delete(dir)
                                return FileVisitResult.CONTINUE
                            }
                        })
                    } else {
                        Files.delete(dirPath)
                    }

                    logger.info("目录删除成功: {} (递归: {})", directoryPath, recursive)
                    true
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "目录删除失败", e)
                }
            }
        }
    }

    override fun getStorageUsage(): Mono<StorageUsage> {
        return Mono.fromCallable {
            readWriteLock.read {
                try {
                    var totalSize = 0L
                    var fileCount = 0L

                    Files.walkFileTree(basePath, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            totalSize += attrs.size()
                            fileCount++
                            return FileVisitResult.CONTINUE
                        }
                    })

                    val fileStore = Files.getFileStore(basePath)
                    val totalSpace = fileStore.totalSpace
                    val freeSpace = fileStore.usableSpace

                    StorageUsage(
                        totalSpace = totalSpace,
                        usedSpace = totalSpace - freeSpace,
                        freeSpace = freeSpace,
                        fileCount = fileCount
                    )
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "获取存储使用情况失败", e)
                }
            }
        }
    }

    override fun validateConfig(config: Map<String, Any>): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                val path = config["path"] as? String
                require(!path.isNullOrBlank()) { "本地存储路径不能为空" }
                
                val testPath = Paths.get(path)
                require(Files.isDirectory(testPath) || Files.notExists(testPath)) { 
                    "路径必须是目录或不存在: $path" 
                }
                
                if (Files.notExists(testPath)) {
                    Files.createDirectories(testPath)
                }
                
                // 测试读写权限
                val testFile = testPath.resolve("test_write_permission.tmp")
                Files.write(testFile, "test".toByteArray())
                Files.delete(testFile)
                
                true
            } catch (e: Exception) {
                logger.error("本地存储配置验证失败", e)
                false
            }
        }
    }

    override fun getFileChecksum(filePath: String): Mono<String> {
        return Mono.fromCallable {
            validateFilePath(filePath)

            readWriteLock.read {
                val targetPath = resolveFilePath(filePath)
                
                if (!Files.exists(targetPath)) {
                    throw CustomFileNotFoundException(filePath)
                }

                try {
                    val digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM)
                    Files.newInputStream(targetPath).use { inputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                    }
                    bytesToHex(digest.digest())
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "计算文件校验和失败", e)
                }
            }
        }
    }

    override fun cleanup(olderThanDays: Int): Mono<Long> {
        return Mono.fromCallable {
            readWriteLock.write {
                val cutoffTime = Instant.now().minusSeconds(olderThanDays * 24 * 3600L)
                var deletedCount = 0L

                try {
                    Files.walkFileTree(basePath, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            if (attrs.lastModifiedTime().toInstant().isBefore(cutoffTime)) {
                                try {
                                    Files.delete(file)
                                    deletedCount++
                                } catch (e: IOException) {
                                    logger.warn("清理文件失败: {}", file, e)
                                }
                            }
                            return FileVisitResult.CONTINUE
                        }
                    })

                    logger.info("清理完成，删除了 {} 个过期文件", deletedCount)
                    deletedCount
                } catch (e: IOException) {
                    throw StorageConnectionException("LOCAL", "文件清理失败", e)
                }
            }
        }
    }

    // 私有辅助方法

    private fun validateFilePath(filePath: String) {
        require(filePath.isNotBlank()) { "文件路径不能为空" }
        require(!filePath.contains("..")) { "文件路径不能包含相对路径: $filePath" }
        require(!filePath.startsWith("/")) { "文件路径不能以/开头: $filePath" }
    }

    private fun validateFileSize(fileSize: Long) {
        require(fileSize > 0) { "文件大小必须大于0" }
        require(fileSize <= maxFileSize) { "文件大小超过限制: $fileSize > $maxFileSize" }
    }

    private fun validateContentType(contentType: String) {
        if (allowedContentTypes.isNotEmpty() && !allowedContentTypes.contains(contentType)) {
            throw UnsupportedFileTypeException(contentType, null, allowedContentTypes)
        }
    }

    private fun resolveFilePath(filePath: String): Path {
        return basePath.resolve(filePath).normalize()
    }

    private fun createFileInfo(path: Path, attrs: BasicFileAttributes): FileInfo {
        val relativePath = basePath.relativize(path).toString()
        return FileInfo(
            path = relativePath,
            name = path.fileName.toString(),
            size = if (attrs.isDirectory) 0L else attrs.size(),
            lastModified = attrs.lastModifiedTime().toMillis(),
            isDirectory = attrs.isDirectory,
            contentType = if (attrs.isDirectory) null else Files.probeContentType(path)
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun isAvailable(): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                // 检查基础目录是否存在且可访问
                if (!Files.exists(basePath)) {
                    logger.warn("本地存储基础目录不存在: {}", basePath)
                    return@fromCallable false
                }

                if (!Files.isDirectory(basePath)) {
                    logger.warn("本地存储路径不是目录: {}", basePath)
                    return@fromCallable false
                }

                if (!Files.isReadable(basePath) || !Files.isWritable(basePath)) {
                    logger.warn("本地存储目录无读写权限: {}", basePath)
                    return@fromCallable false
                }

                // 尝试创建临时文件来测试写入权限
                val testFile = basePath.resolve(".storage_test_${System.currentTimeMillis()}")
                try {
                    Files.write(testFile, "test".toByteArray())
                    Files.delete(testFile)
                    true
                } catch (e: Exception) {
                    logger.warn("本地存储写入测试失败: {}", basePath, e)
                    false
                }
            } catch (e: Exception) {
                logger.error("检查本地存储可用性失败", e)
                false
            }
        }
    }
} 