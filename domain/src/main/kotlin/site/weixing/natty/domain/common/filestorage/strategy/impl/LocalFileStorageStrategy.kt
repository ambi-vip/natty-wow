package site.weixing.natty.domain.common.filestorage.strategy.impl

import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.file.StorageInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage
import site.weixing.natty.domain.common.filestorage.exception.*
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.jvm.java

/**
 * 本地文件存储策略实现（全流式）
 */
class LocalFileStorageStrategy(
    private val baseDirectory: String,
    private val maxFileSize: Long = 100 * 1024 * 1024, // 100MB
    private val allowedContentTypes: Set<String> = emptySet(),
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

    init {
        try {
            Files.createDirectories(basePath)
            logger.info("本地存储初始化完成，基础目录: {}", basePath)
        } catch (e: Exception) {
            throw StorageConfigurationException("baseDirectory", "无法创建基础目录: $baseDirectory", e)
        }
    }

    private fun resolveFilePath(filePath: String): Path {
        return basePath.resolve(filePath).normalize()
    }

    override fun uploadFile(
        filePath: String,
        dataBufferFlux: Flux<DataBuffer>,
        contentType: String,
        metadata: Map<String, String>
    ): Mono<StorageInfo> {
        return Mono.defer {
            val start = System.currentTimeMillis()
            val targetPath = resolveFilePath(filePath)
            Files.createDirectories(targetPath.parent)
            logger.info("[LocalFileStorageStrategy] uploadFile入口: $filePath, 目标: $targetPath")
            
            val writeOperation: Mono<Void> = DataBufferUtils.write(dataBufferFlux, targetPath)
                .doOnSubscribe { logger.info("[LocalFileStorageStrategy] DataBufferUtils.write 开始: $filePath") }
                .doFinally { _ -> logger.info("[LocalFileStorageStrategy] DataBufferUtils.write 完成: $filePath, 耗时: ${System.currentTimeMillis() - start} ms") }
            
            writeOperation.then(Mono.fromCallable {
                val end = System.currentTimeMillis()
                logger.info("[LocalFileStorageStrategy] uploadFile 总耗时: ${end - start} ms, $filePath")
                StorageInfo.local(
                    storagePath = filePath,
                    etag = null // 可选：如需校验和可异步计算
                )
            })
        }
    }

    override fun downloadFile(filePath: String): Flux<DataBuffer> {
        val targetPath = resolveFilePath(filePath)
        val bufferFactory = DefaultDataBufferFactory()
        val bufferSize = BUFFER_SIZE
        return DataBufferUtils.read(targetPath, bufferFactory, bufferSize)
    }

    // 其余元数据/管理接口保持不变
    override fun deleteFile(filePath: String): Mono<Boolean> = Mono.fromCallable {
        val targetPath = resolveFilePath(filePath)
        try {
            Files.deleteIfExists(targetPath)
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 删除文件失败: $filePath", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun existsFile(filePath: String): Mono<Boolean> = Mono.fromCallable {
        Files.exists(resolveFilePath(filePath))
    }.subscribeOn(Schedulers.boundedElastic())

    override fun getFileSize(filePath: String): Mono<Long> = Mono.fromCallable {
        Files.size(resolveFilePath(filePath))
    }.subscribeOn(Schedulers.boundedElastic())

    override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?): Mono<String> = Mono.fromCallable {
        val path = resolveFilePath(filePath)
        path.toUri().toString()
    }.subscribeOn(Schedulers.boundedElastic())

    override fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> = Mono.fromCallable {
        val src = resolveFilePath(sourcePath)
        val dst = resolveFilePath(destPath)
        try {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 复制文件失败: $sourcePath -> $destPath", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> = Mono.fromCallable {
        val src = resolveFilePath(sourcePath)
        val dst = resolveFilePath(destPath)
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 移动文件失败: $sourcePath -> $destPath", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun listFiles(directoryPath: String, recursive: Boolean): Mono<List<FileInfo>> = Mono.fromCallable {
        val dir = resolveFilePath(directoryPath)
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return@fromCallable emptyList<FileInfo>()
        val result = mutableListOf<FileInfo>()
        val maxDepth = if (recursive) Integer.MAX_VALUE else 1
        Files.walk(dir, maxDepth).use { stream ->
            stream.forEach { path ->
                val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
                result.add(
                    FileInfo(
                        path = basePath.relativize(path).toString(),
                        name = path.fileName.toString(),
                        size = attrs.size(),
                        lastModified = attrs.lastModifiedTime().toMillis(),
                        isDirectory = attrs.isDirectory,
                        contentType = null,
                        etag = null
                    )
                )
            }
        }
        result
    }.subscribeOn(Schedulers.boundedElastic())

    override fun createDirectory(directoryPath: String): Mono<Boolean> = Mono.fromCallable {
        val dir = resolveFilePath(directoryPath)
        try {
            Files.createDirectories(dir)
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 创建目录失败: $directoryPath", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun deleteDirectory(directoryPath: String, recursive: Boolean): Mono<Boolean> = Mono.fromCallable {
        val dir = resolveFilePath(directoryPath)
        if (!Files.exists(dir)) return@fromCallable false
        try {
            if (recursive) {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            } else {
                Files.deleteIfExists(dir)
            }
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 删除目录失败: $directoryPath", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun getStorageUsage(): Mono<StorageUsage> = Mono.fromCallable {
        var totalSize = 0L
        var fileCount = 0L
        Files.walk(basePath).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { path ->
                totalSize += Files.size(path)
                fileCount++
            }
        }
        val fileStore = Files.getFileStore(basePath)
        StorageUsage(
            totalSpace = fileStore.totalSpace,
            usedSpace = fileStore.totalSpace - fileStore.unallocatedSpace,
            freeSpace = fileStore.unallocatedSpace,
            fileCount = fileCount
        )
    }.subscribeOn(Schedulers.boundedElastic())

    override fun validateConfig(config: Map<String, Any>): Mono<Boolean> = Mono.fromCallable {
        try {
            Files.createDirectories(basePath)
            Files.isWritable(basePath)
        } catch (e: Exception) {
            logger.warn("[LocalFileStorageStrategy] 配置校验失败", e)
            return@fromCallable false
        }
        true
    }.subscribeOn(Schedulers.boundedElastic())

    override fun getFileChecksum(filePath: String): Mono<String> = Mono.fromCallable {
        val path = resolveFilePath(filePath)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.subscribeOn(Schedulers.boundedElastic())

    override fun cleanup(olderThanDays: Int): Mono<Long> = Mono.fromCallable {
        val cutoff = java.time.Instant.now().minusSeconds(olderThanDays * 24L * 3600L)
        var deleted = 0L
        Files.walk(basePath).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { path ->
                val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
                if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                    try {
                        Files.deleteIfExists(path)
                        deleted++
                    } catch (e: Exception) {
                        logger.warn("[LocalFileStorageStrategy] 清理文件失败: ${'$'}path", e)
                    }
                }
            }
        }
        deleted
    }.subscribeOn(Schedulers.boundedElastic())
} 