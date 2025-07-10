package site.weixing.natty.domain.common.filestorage.temp

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import site.weixing.natty.domain.common.filestorage.exception.TemporaryFileNotFoundException
import site.weixing.natty.domain.common.filestorage.exception.TemporaryFileExpiredException
import site.weixing.natty.domain.common.filestorage.exception.TemporaryFileCreationException
import site.weixing.natty.domain.common.filestorage.exception.TemporaryFileAccessException
import site.weixing.natty.domain.common.filestorage.exception.TemporaryFileSizeExceededException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 本地临时文件管理器实现
 * 
 * 基于本地文件系统的临时文件管理器，提供高性能的文件创建、访问和清理功能。
 * 特性：
 * 1. 线程安全 - 使用 ConcurrentHashMap 管理文件引用
 * 2. 自动清理 - 定期扫描和清理过期文件
 * 3. 异常安全 - 完整的错误处理和恢复机制
 * 4. 性能优化 - 异步I/O和响应式编程模型
 */
class LocalTemporaryFileManager(
    private val tempDirectory: String = "${System.getProperty("user.dir")}/storage/temp",
    private val defaultExpirationHours: Long = 1L,
    private val maxFileSize: Long = 5L * 1024 * 1024 * 1024, // 5GB
    private val cleanupIntervalMinutes: Long = 30L
) : TemporaryFileManager {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    private val activeReferences = ConcurrentHashMap<String, TemporaryFileReference>()
    private val cleanupScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "temp-file-cleanup").apply { isDaemon = true }
    }
    
    @PostConstruct
    fun initialize() {
        try {
            // 创建临时文件目录
            val tempPath = Paths.get(tempDirectory)
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath)
                logger.info { "创建临时文件目录: $tempDirectory" }
            }
            
            // 启动定期清理任务
            cleanupScheduler.scheduleAtFixedRate(
                { runCleanupTask() },
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
            )
            
            logger.info { "临时文件管理器初始化完成: 目录=$tempDirectory, 过期时间=${defaultExpirationHours}小时, 最大文件大小=${maxFileSize}字节" }
        } catch (e: Exception) {
            logger.error(e) { "临时文件管理器初始化失败" }
            throw TemporaryFileCreationException("临时文件管理器初始化失败", e)
        }
    }
    
    override fun createTemporaryFile(
        originalFileName: String,
        fileSize: Long,
        contentType: String,
        inputStream: InputStream
    ): Mono<TemporaryFileReference> {
        return Mono.fromCallable {
            // 参数验证
            require(originalFileName.isNotBlank()) { "文件名不能为空" }
            require(fileSize > 0) { "文件大小必须大于0" }
            require(fileSize <= maxFileSize) { "文件大小超过限制: $fileSize > $maxFileSize" }
            require(contentType.isNotBlank()) { "内容类型不能为空" }
            
            val referenceId = UUID.randomUUID().toString()
            val now = Instant.now()
            val expiresAt = now.plus(defaultExpirationHours, ChronoUnit.HOURS)
            
            // 创建临时文件路径
            val tempPath = Paths.get(tempDirectory, "${referenceId}_${originalFileName}")
            
            logger.debug { "开始创建临时文件: $referenceId, 文件名=$originalFileName, 大小=${fileSize}字节" }
            
            try {
                // 写入文件内容并计算校验和
                val checksum = writeFileWithChecksum(inputStream, tempPath, fileSize)
                
                // 创建文件引用
                val reference = TemporaryFileReference(
                    referenceId = referenceId,
                    originalFileName = originalFileName,
                    fileSize = fileSize,
                    contentType = contentType,
                    temporaryPath = tempPath.toString(),
                    createdAt = now,
                    expiresAt = expiresAt,
                    checksum = checksum
                )
                
                // 注册引用
                activeReferences[referenceId] = reference
                
                logger.info { "临时文件创建成功: $referenceId, 路径=${tempPath}, 校验和=$checksum" }
                reference
                
            } catch (e: Exception) {
                // 清理可能创建的临时文件
                try {
                    Files.deleteIfExists(tempPath)
                } catch (cleanupException: Exception) {
                    logger.warn(cleanupException) { "清理失败的临时文件时出错: $tempPath" }
                }
                
                logger.error(e) { "创建临时文件失败: $referenceId" }
                throw TemporaryFileCreationException("创建临时文件失败: ${e.message}", e)
            }
        }
        .subscribeOn(Schedulers.boundedElastic())
    }
    
    override fun getFileStream(referenceId: String): Mono<InputStream> {
        return Mono.fromCallable {
            logger.debug { "获取临时文件流: $referenceId" }
            
            val reference = activeReferences[referenceId]
                ?: throw TemporaryFileNotFoundException(referenceId)
            
            // 检查是否过期
            if (reference.isExpired()) {
                logger.warn { "临时文件已过期: $referenceId, 过期时间=${reference.expiresAt}" }
                // 清理过期文件
                cleanupExpiredReference(referenceId, reference)
                throw TemporaryFileExpiredException(referenceId)
            }
            
            try {
                val path = Paths.get(reference.temporaryPath)
                if (!Files.exists(path)) {
                    logger.error { "临时文件物理文件不存在: $referenceId, 路径=${reference.temporaryPath}" }
                    activeReferences.remove(referenceId)
                    throw TemporaryFileNotFoundException(referenceId)
                }
                
                FileInputStream(path.toFile()) as InputStream
            } catch (e: NoSuchFileException) {
                logger.error { "临时文件不存在: $referenceId, 路径=${reference.temporaryPath}" }
                activeReferences.remove(referenceId)
                throw TemporaryFileNotFoundException(referenceId)
            } catch (e: Exception) {
                logger.error(e) { "访问临时文件失败: $referenceId" }
                throw TemporaryFileAccessException(referenceId, cause = e)
            }
        }
        .subscribeOn(Schedulers.boundedElastic())
    }
    
    override fun deleteTemporaryFile(referenceId: String): Mono<Boolean> {
        return Mono.fromCallable {
            logger.debug { "删除临时文件: $referenceId" }
            
            val reference = activeReferences.remove(referenceId)
            if (reference == null) {
                logger.debug { "临时文件引用不存在，可能已被删除: $referenceId" }
                return@fromCallable false
            }
            
            try {
                val path = Paths.get(reference.temporaryPath)
                val deleted = Files.deleteIfExists(path)
                
                if (deleted) {
                    logger.info { "临时文件删除成功: $referenceId, 路径=${reference.temporaryPath}" }
                } else {
                    logger.warn { "临时文件物理文件不存在: $referenceId, 路径=${reference.temporaryPath}" }
                }
                
                true
            } catch (e: Exception) {
                logger.error(e) { "删除临时文件失败: $referenceId, 路径=${reference.temporaryPath}" }
                // 即使物理删除失败，也认为逻辑删除成功
                true
            }
        }
        .subscribeOn(Schedulers.boundedElastic())
    }
    
    override fun cleanupExpiredFiles(): Mono<Long> {
        return Mono.fromCallable {
            logger.debug { "开始清理过期临时文件" }
            
            val now = Instant.now()
            var cleanedCount = 0L
            
            val expiredReferences = activeReferences.filterValues { it.isExpired() }
            
            for ((referenceId, reference) in expiredReferences) {
                try {
                    activeReferences.remove(referenceId)
                    val path = Paths.get(reference.temporaryPath)
                    if (Files.deleteIfExists(path)) {
                        cleanedCount++
                        logger.debug { "清理过期临时文件: $referenceId, 过期时间=${reference.expiresAt}" }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "清理过期临时文件失败: $referenceId" }
                }
            }
            
            if (cleanedCount > 0) {
                logger.info { "清理过期临时文件完成: 清理数量=$cleanedCount" }
            }
            
            cleanedCount
        }
        .subscribeOn(Schedulers.boundedElastic())
    }
    
    override fun getTemporaryFileReference(referenceId: String): Mono<TemporaryFileReference> {
        return Mono.fromCallable {
            val reference = activeReferences[referenceId]
                ?: throw TemporaryFileNotFoundException(referenceId)
            
            if (reference.isExpired()) {
                cleanupExpiredReference(referenceId, reference)
                throw TemporaryFileExpiredException(referenceId)
            }
            
            reference
        }
    }
    
    override fun isTemporaryFileValid(referenceId: String): Mono<Boolean> {
        return Mono.fromCallable {
            val reference = activeReferences[referenceId] ?: return@fromCallable false
            !reference.isExpired()
        }
    }
    
    @PreDestroy
    private fun shutdown() {
        logger.info { "关闭临时文件管理器" }
        cleanupScheduler.shutdown()
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cleanupScheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
    
    private fun writeFileWithChecksum(inputStream: InputStream, outputPath: Path, expectedSize: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        
        Files.newOutputStream(outputPath).use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
                totalBytes += bytesRead
                
                // 检查文件大小是否超过预期
                if (totalBytes > expectedSize) {
                    throw TemporaryFileSizeExceededException(totalBytes, expectedSize)
                }
            }
        }
        
        // 验证最终大小
        if (totalBytes != expectedSize) {
            logger.warn { "文件大小不匹配: 期望=${expectedSize}字节, 实际=${totalBytes}字节" }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    private fun cleanupExpiredReference(referenceId: String, reference: TemporaryFileReference) {
        activeReferences.remove(referenceId)
        try {
            Files.deleteIfExists(Paths.get(reference.temporaryPath))
        } catch (e: Exception) {
            logger.warn(e) { "清理过期文件失败: $referenceId" }
        }
    }
    
    private fun runCleanupTask() {
        try {
            cleanupExpiredFiles().subscribe(
                { cleanedCount ->
                    if (cleanedCount > 0) {
                        logger.debug { "定期清理任务完成: 清理了 $cleanedCount 个过期文件" }
                    }
                },
                { error ->
                    logger.error(error) { "定期清理任务失败" }
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "定期清理任务异常" }
        }
    }
} 