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
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory

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
    private val readWriteLock = ReentrantReadWriteLock()

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
    override fun deleteFile(filePath: String): Mono<Boolean> = Mono.error(NotImplementedError("deleteFile 未实现"))
    override fun existsFile(filePath: String): Mono<Boolean> = Mono.error(NotImplementedError("existsFile 未实现"))
    override fun getFileSize(filePath: String): Mono<Long> = Mono.error(NotImplementedError("getFileSize 未实现"))
    override fun getFileUrl(filePath: String, expirationTimeInSeconds: Long?): Mono<String> = Mono.error(NotImplementedError("getFileUrl 未实现"))
    override fun copyFile(sourcePath: String, destPath: String): Mono<Boolean> = Mono.error(NotImplementedError("copyFile 未实现"))
    override fun moveFile(sourcePath: String, destPath: String): Mono<Boolean> = Mono.error(NotImplementedError("moveFile 未实现"))
    override fun listFiles(directoryPath: String, recursive: Boolean): Mono<List<FileInfo>> = Mono.error(NotImplementedError("listFiles 未实现"))
    override fun createDirectory(directoryPath: String): Mono<Boolean> = Mono.error(NotImplementedError("createDirectory 未实现"))
    override fun deleteDirectory(directoryPath: String, recursive: Boolean): Mono<Boolean> = Mono.error(NotImplementedError("deleteDirectory 未实现"))
    override fun getStorageUsage(): Mono<StorageUsage> = Mono.error(NotImplementedError("getStorageUsage 未实现"))
    override fun validateConfig(config: Map<String, Any>): Mono<Boolean> = Mono.error(NotImplementedError("validateConfig 未实现"))
    override fun getFileChecksum(filePath: String): Mono<String> = Mono.error(NotImplementedError("getFileChecksum 未实现"))
    override fun cleanup(olderThanDays: Int): Mono<Long> = Mono.error(NotImplementedError("cleanup 未实现"))
} 