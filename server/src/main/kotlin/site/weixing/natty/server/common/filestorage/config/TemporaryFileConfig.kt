package site.weixing.natty.server.common.filestorage.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * 临时文件配置类
 *
 * 用于配置临时文件管理器的各项参数，支持通过 application.yml 进行配置。
 * 配置前缀：natty.filestorage.temp
 *
 * 示例配置：
 * ```yaml
 * natty:
 *   filestorage:
 *     temp:
 *       directory: "./storage/temp"
 *       expiration-hours: 2
 *       max-file-size: 10737418240  # 10GB
 *       cleanup-interval-minutes: 15
 *       enable-auto-cleanup: true
 *       max-concurrent-uploads: 100
 * ```
 */
@Configuration
@ConfigurationProperties(prefix = "natty.filestorage.temp")
@Validated
data class TemporaryFileConfig(

    /**
     * 临时文件存储目录
     * 默认值：当前工作目录下的 storage/temp
     */
    @field:NotBlank(message = "临时文件目录不能为空")
    var directory: String = "${System.getProperty("user.dir")}/storage/temp",

    /**
     * 临时文件过期时间（小时）
     * 默认值：1小时
     * 最小值：1小时
     */
    @field:Min(value = 1, message = "过期时间不能小于1小时")
    var expirationHours: Long = 1L,

    /**
     * 最大文件大小（字节）
     * 默认值：50MB
     * 最小值：1MB
     */
    @field:Min(value = 1048576, message = "最大文件大小不能小于1MB")
    var maxFileSize: Long = 50L * 1024 * 1024,

    /**
     * 自动清理间隔时间（分钟）
     * 默认值：30分钟
     * 最小值：5分钟
     */
    @field:Min(value = 5, message = "清理间隔不能小于5分钟")
    var cleanupIntervalMinutes: Long = 30L,

    /**
     * 是否启用自动清理
     * 默认值：true
     */
    var enableAutoCleanup: Boolean = true,

    /**
     * 最大并发上传数量
     * 默认值：100
     * 最小值：1
     */
    @field:Min(value = 1, message = "最大并发上传数量不能小于1")
    var maxConcurrentUploads: Int = 100,

    /**
     * 启用文件校验和
     * 默认值：true
     */
    var enableChecksum: Boolean = true,

    /**
     * 启用文件大小验证
     * 默认值：true
     */
    var enableSizeValidation: Boolean = true,

    /**
     * 启用详细日志
     * 默认值：false（生产环境建议关闭）
     */
    var enableVerboseLogging: Boolean = false,

    /**
     * 文件写入缓冲区大小（字节）
     * 默认值：8KB
     * 最小值：1KB
     */
    @field:Min(value = 1024, message = "缓冲区大小不能小于1KB")
    var bufferSize: Int = 8192
) {

    /**
     * 获取格式化的最大文件大小描述
     */
    fun getFormattedMaxFileSize(): String {
        return when {
            maxFileSize >= 1024 * 1024 * 1024 -> "${maxFileSize / (1024 * 1024 * 1024)}GB"
            maxFileSize >= 1024 * 1024 -> "${maxFileSize / (1024 * 1024)}MB"
            maxFileSize >= 1024 -> "${maxFileSize / 1024}KB"
            else -> "${maxFileSize}B"
        }
    }

    /**
     * 获取格式化的过期时间描述
     */
    fun getFormattedExpirationTime(): String {
        return when {
            expirationHours >= 24 -> "${expirationHours / 24}天"
            else -> "${expirationHours}小时"
        }
    }

    /**
     * 获取格式化的清理间隔描述
     */
    fun getFormattedCleanupInterval(): String {
        return when {
            cleanupIntervalMinutes >= 60 -> "${cleanupIntervalMinutes / 60}小时"
            else -> "${cleanupIntervalMinutes}分钟"
        }
    }

    /**
     * 验证配置的有效性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (directory.isBlank()) {
            errors.add("临时文件目录不能为空")
        }

        if (expirationHours < 1) {
            errors.add("过期时间不能小于1小时")
        }

        if (maxFileSize < 1024 * 1024) {
            errors.add("最大文件大小不能小于1MB")
        }

        if (cleanupIntervalMinutes < 5) {
            errors.add("清理间隔不能小于5分钟")
        }

        if (maxConcurrentUploads < 1) {
            errors.add("最大并发上传数量不能小于1")
        }

        if (bufferSize < 1024) {
            errors.add("缓冲区大小不能小于1KB")
        }

        return errors
    }

    /**
     * 打印配置摘要
     */
    fun printSummary(): String {
        return """
            |临时文件配置摘要:
            |  存储目录: $directory
            |  过期时间: ${getFormattedExpirationTime()}
            |  最大文件大小: ${getFormattedMaxFileSize()}
            |  清理间隔: ${getFormattedCleanupInterval()}
            |  自动清理: ${if (enableAutoCleanup) "启用" else "禁用"}
            |  最大并发: $maxConcurrentUploads
            |  文件校验: ${if (enableChecksum) "启用" else "禁用"}
            |  大小验证: ${if (enableSizeValidation) "启用" else "禁用"}
            |  详细日志: ${if (enableVerboseLogging) "启用" else "禁用"}
            |  缓冲区大小: ${bufferSize}字节
        """.trimMargin()
    }
} 