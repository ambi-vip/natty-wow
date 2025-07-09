package site.weixing.natty.server.common.filestorage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import site.weixing.natty.domain.common.filestorage.validation.FileReferenceValidator

/**
 * 定时清理过期的临时文件引用
 */
@Component
class TemporaryFileCleanupScheduler(
    private val fileReferenceValidator: FileReferenceValidator
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * 每小时清理一次无效的临时文件引用
     */
    @Scheduled(cron = "0 0 * * * ?")
    fun cleanupInvalidReferences() {
        logger.info { "[定时任务] 开始清理无效的临时文件引用..." }
        try {
            val cleanedCount = fileReferenceValidator.cleanupInvalidReferences().block() ?: 0
            logger.info { "[定时任务] 清理完成，无效引用数量: $cleanedCount" }
        } catch (ex: Exception) {
            logger.error(ex) { "[定时任务] 清理无效临时文件引用时发生异常" }
        }
    }
} 