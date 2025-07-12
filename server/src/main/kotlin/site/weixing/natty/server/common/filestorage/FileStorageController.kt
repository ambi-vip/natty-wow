package site.weixing.natty.server.common.filestorage

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.service.FileStorageService
import site.weixing.natty.domain.common.filestorage.strategy.FileInfo
import site.weixing.natty.domain.common.filestorage.strategy.StorageUsage

@RestController
@RequestMapping("/api/files")
class FileStorageController(
    private val fileStorageService: FileStorageService
) {
    /**
     * 下载文件
     */
    @GetMapping("/download")
    fun downloadFile(
        @RequestParam filePath: String
    ): ResponseEntity<Flux<ByteArray>> {
        val flux = fileStorageService.downloadFile(filePath).map { dataBuffer ->
            val bytes = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(bytes)
            bytes
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filePath.substringAfterLast('/')}\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(flux)
    }

    /**
     * 删除文件
     */
    @DeleteMapping
    fun deleteFile(@RequestParam filePath: String): Mono<ResponseEntity<Boolean>> {
        return fileStorageService.deleteFile(filePath)
            .map { deleted -> ResponseEntity.ok(deleted) }
    }

    /**
     * 批量删除文件（暂未实现，返回 501）
     */
    @DeleteMapping("/batch")
    fun batchDeleteFiles(@RequestBody filePaths: List<String>): Mono<ResponseEntity<Any>> {
        return Mono.just(ResponseEntity.status(501).body("批量删除未实现"))
    }

    /**
     * 复制文件
     */
    @PostMapping("/copy")
    fun copyFile(
        @RequestParam sourcePath: String,
        @RequestParam destPath: String
    ): Mono<ResponseEntity<Boolean>> {
        return fileStorageService.copyFile(sourcePath, destPath)
            .map { result -> ResponseEntity.ok(result) }
    }

    /**
     * 移动文件
     */
    @PostMapping("/move")
    fun moveFile(
        @RequestParam sourcePath: String,
        @RequestParam destPath: String
    ): Mono<ResponseEntity<Boolean>> {
        return fileStorageService.moveFile(sourcePath, destPath)
            .map { result -> ResponseEntity.ok(result) }
    }

    /**
     * 获取存储用量
     */
    @GetMapping("/usage")
    fun getStorageUsage(): Mono<ResponseEntity<StorageUsage>> {
        return fileStorageService.getStorageUsage()
            .map { usage -> ResponseEntity.ok(usage) }
    }

    /**
     * 列出目录文件
     */
    @GetMapping("/list")
    fun listFiles(
        @RequestParam directoryPath: String,
        @RequestParam(required = false, defaultValue = "false") recursive: Boolean
    ): Mono<ResponseEntity<List<FileInfo>>> {
        return fileStorageService.listFiles(directoryPath, recursive)
            .map { files -> ResponseEntity.ok(files) }
    }

    /**
     * 校验文件完整性（未实现，返回 501）
     */
    @PostMapping("/validate")
    fun validateFileIntegrity(
        @RequestParam filePath: String,
        @RequestParam expectedChecksum: String
    ): Mono<ResponseEntity<Boolean>> {
        return Mono.just(ResponseEntity.status(501).body(false))
    }

    /**
     * 清理过期文件
     */
    @PostMapping("/cleanup")
    fun cleanupExpiredFiles(
        @RequestParam(required = false, defaultValue = "7") olderThanDays: Int
    ): Mono<ResponseEntity<Long>> {
        return fileStorageService.cleanup(olderThanDays)
            .map { count -> ResponseEntity.ok(count) }
    }

    /**
     * 清理存储策略缓存（未实现，返回 200）
     */
    @PostMapping("/clear-cache")
    fun clearStrategyCache(): ResponseEntity<String> {
        return ResponseEntity.ok("缓存清理接口未实现")
    }
} 