package site.weixing.natty.domain.common.filestorage.router.rules

import reactor.core.publisher.Mono
import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.router.FileUploadContext
import site.weixing.natty.domain.common.filestorage.strategy.FileStorageStrategy

/**
 * 基于内容类型的路由规则
 * 根据文件类型选择最适合的存储策略
 */
class ContentTypeBasedRule(
    private val availableStrategies: Map<StorageProvider, FileStorageStrategy>,
    private val configuration: ContentTypeRuleConfiguration = ContentTypeRuleConfiguration()
) : StorageRoutingRule {

    override fun evaluate(context: FileUploadContext): Mono<RoutingDecision> {
        return Mono.fromCallable {
            val contentType = context.contentType
            val fileCategory = categorizeContentType(contentType)
            val preferredProvider = selectProviderByContentType(fileCategory, context)
            val strategy = availableStrategies[preferredProvider]
            
            if (strategy != null) {
                RoutingDecision(
                    strategy = strategy,
                    score = calculateScore(fileCategory, preferredProvider, context),
                    reason = buildDecisionReason(fileCategory, preferredProvider, contentType),
                    ruleType = "CONTENT_TYPE",
                    metadata = mapOf(
                        "contentType" to contentType,
                        "fileCategory" to fileCategory.name,
                        "isPublic" to context.isPublic
                    )
                )
            } else {
                // 回退策略
                val fallbackStrategy = availableStrategies[StorageProvider.LOCAL]
                    ?: throw IllegalStateException("本地存储策略不可用")
                
                RoutingDecision(
                    strategy = fallbackStrategy,
                    score = 55, // 回退策略分数
                    reason = "首选存储策略不可用，回退到本地存储",
                    ruleType = "CONTENT_TYPE_FALLBACK"
                )
            }
        }
    }

    override fun getWeight(): Int = 70 // 内容类型是重要的路由因子

    /**
     * 对内容类型进行分类
     */
    private fun categorizeContentType(contentType: String): FileCategory {
        val lowerType = contentType.lowercase()
        
        return when {
            // 图片文件
            lowerType.startsWith("image/") -> {
                when {
                    lowerType.contains("svg") -> FileCategory.VECTOR_IMAGE
                    lowerType.contains("gif") -> FileCategory.ANIMATED_IMAGE
                    lowerType.contains("webp") || lowerType.contains("avif") -> FileCategory.MODERN_IMAGE
                    else -> FileCategory.RASTER_IMAGE
                }
            }
            
            // 视频文件
            lowerType.startsWith("video/") -> {
                when {
                    lowerType.contains("mp4") || lowerType.contains("webm") -> FileCategory.WEB_VIDEO
                    lowerType.contains("mov") || lowerType.contains("avi") -> FileCategory.RAW_VIDEO
                    else -> FileCategory.OTHER_VIDEO
                }
            }
            
            // 音频文件
            lowerType.startsWith("audio/") -> FileCategory.AUDIO
            
            // 文档文件
            lowerType.startsWith("text/") -> FileCategory.TEXT_DOCUMENT
            lowerType.contains("pdf") -> FileCategory.PDF_DOCUMENT
            lowerType.contains("word") || lowerType.contains("docx") -> FileCategory.OFFICE_DOCUMENT
            lowerType.contains("excel") || lowerType.contains("spreadsheet") -> FileCategory.SPREADSHEET
            lowerType.contains("powerpoint") || lowerType.contains("presentation") -> FileCategory.PRESENTATION
            
            // 压缩文件
            lowerType.contains("zip") || lowerType.contains("rar") || 
            lowerType.contains("7z") || lowerType.contains("tar") -> FileCategory.ARCHIVE
            
            // 代码文件
            lowerType.contains("javascript") || lowerType.contains("json") ||
            lowerType.contains("xml") || lowerType.contains("yaml") -> FileCategory.CODE
            
            // 二进制文件
            lowerType.startsWith("application/octet-stream") -> FileCategory.BINARY
            
            // 其他
            else -> FileCategory.OTHER
        }
    }

    /**
     * 根据内容类型选择存储提供商
     */
    private fun selectProviderByContentType(category: FileCategory, context: FileUploadContext): StorageProvider {
        return when (category) {
            // 图片文件 - 如果是公开的，优先云存储（便于CDN）
            FileCategory.RASTER_IMAGE, FileCategory.MODERN_IMAGE -> {
                if (context.isPublic) {
                    StorageProvider.S3 // CDN友好
                } else {
                    StorageProvider.LOCAL // 私有图片本地存储
                }
            }
            
            FileCategory.VECTOR_IMAGE -> StorageProvider.LOCAL // SVG通常较小，本地存储快
            FileCategory.ANIMATED_IMAGE -> StorageProvider.S3 // GIF通常需要CDN加速
            
            // 视频文件 - 大多数情况优先云存储
            FileCategory.WEB_VIDEO, FileCategory.RAW_VIDEO, FileCategory.OTHER_VIDEO -> {
                StorageProvider.ALIYUN_OSS // 视频文件大，成本优先
            }
            
            // 音频文件 - 云存储
            FileCategory.AUDIO -> StorageProvider.S3
            
            // 文档文件 - 根据公开性选择
            FileCategory.TEXT_DOCUMENT, FileCategory.PDF_DOCUMENT -> {
                if (context.isPublic) StorageProvider.S3 else StorageProvider.LOCAL
            }
            
            FileCategory.OFFICE_DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> {
                StorageProvider.LOCAL // 办公文档通常私有且需要快速访问
            }
            
            // 压缩文件 - 通常较大，云存储
            FileCategory.ARCHIVE -> StorageProvider.ALIYUN_OSS
            
            // 代码文件 - 本地存储快速
            FileCategory.CODE -> StorageProvider.LOCAL
            
            // 二进制文件 - 根据大小决策
            FileCategory.BINARY -> {
                if (context.isLargeFile()) StorageProvider.ALIYUN_OSS else StorageProvider.LOCAL
            }
            
            // 其他文件 - 默认本地存储
            FileCategory.OTHER -> StorageProvider.LOCAL
        }
    }

    /**
     * 计算路由决策分数
     */
    private fun calculateScore(category: FileCategory, provider: StorageProvider, context: FileUploadContext): Int {
        var baseScore = when (category) {
            FileCategory.RASTER_IMAGE, FileCategory.MODERN_IMAGE -> {
                when (provider) {
                    StorageProvider.S3 -> if (context.isPublic) 90 else 75
                    StorageProvider.LOCAL -> if (context.isPublic) 70 else 85
                    StorageProvider.ALIYUN_OSS -> 80
                }
            }
            
            FileCategory.WEB_VIDEO, FileCategory.RAW_VIDEO -> {
                when (provider) {
                    StorageProvider.ALIYUN_OSS -> 95 // 视频文件OSS最优
                    StorageProvider.S3 -> 85
                    StorageProvider.LOCAL -> 60 // 本地存储不适合大视频
                }
            }
            
            FileCategory.OFFICE_DOCUMENT -> {
                when (provider) {
                    StorageProvider.LOCAL -> 90 // 办公文档本地最快
                    StorageProvider.S3 -> 75
                    StorageProvider.ALIYUN_OSS -> 70
                }
            }
            
            FileCategory.CODE -> {
                when (provider) {
                    StorageProvider.LOCAL -> 95 // 代码文件本地最优
                    StorageProvider.S3 -> 70
                    StorageProvider.ALIYUN_OSS -> 70
                }
            }
            
            else -> 80 // 默认分数
        }
        
        // 根据文件大小调整分数
        if (context.isLargeFile() && provider == StorageProvider.LOCAL) {
            baseScore -= 15 // 大文件本地存储分数降低
        }
        
        if (context.isSmallFile() && provider != StorageProvider.LOCAL) {
            baseScore -= 10 // 小文件云存储分数降低
        }
        
        return maxOf(minOf(baseScore, 100), 0)
    }

    /**
     * 构建决策原因说明
     */
    private fun buildDecisionReason(category: FileCategory, provider: StorageProvider, contentType: String): String {
        val categoryName = when (category) {
            FileCategory.RASTER_IMAGE -> "光栅图片"
            FileCategory.VECTOR_IMAGE -> "矢量图片"
            FileCategory.ANIMATED_IMAGE -> "动画图片"
            FileCategory.MODERN_IMAGE -> "现代图片格式"
            FileCategory.WEB_VIDEO -> "网络视频"
            FileCategory.RAW_VIDEO -> "原始视频"
            FileCategory.OTHER_VIDEO -> "其他视频"
            FileCategory.AUDIO -> "音频文件"
            FileCategory.TEXT_DOCUMENT -> "文本文档"
            FileCategory.PDF_DOCUMENT -> "PDF文档"
            FileCategory.OFFICE_DOCUMENT -> "办公文档"
            FileCategory.SPREADSHEET -> "电子表格"
            FileCategory.PRESENTATION -> "演示文稿"
            FileCategory.ARCHIVE -> "压缩文件"
            FileCategory.CODE -> "代码文件"
            FileCategory.BINARY -> "二进制文件"
            FileCategory.OTHER -> "其他文件"
        }
        
        return "文件类型 $contentType 识别为 $categoryName，适合使用 ${provider.displayName}"
    }
}

/**
 * 文件分类枚举
 */
enum class FileCategory {
    RASTER_IMAGE,      // 光栅图片 (jpg, png, bmp)
    VECTOR_IMAGE,      // 矢量图片 (svg)
    ANIMATED_IMAGE,    // 动画图片 (gif)
    MODERN_IMAGE,      // 现代图片格式 (webp, avif)
    WEB_VIDEO,         // 网络视频 (mp4, webm)
    RAW_VIDEO,         // 原始视频 (mov, avi)
    OTHER_VIDEO,       // 其他视频格式
    AUDIO,             // 音频文件
    TEXT_DOCUMENT,     // 纯文本文档
    PDF_DOCUMENT,      // PDF文档
    OFFICE_DOCUMENT,   // 办公文档 (word, docx)
    SPREADSHEET,       // 电子表格
    PRESENTATION,      // 演示文稿
    ARCHIVE,           // 压缩文件
    CODE,              // 代码文件
    BINARY,            // 二进制文件
    OTHER              // 其他类型
}

/**
 * 内容类型规则配置
 */
data class ContentTypeRuleConfiguration(
    val enableCdnOptimization: Boolean = true,
    val preferCloudForPublicFiles: Boolean = true,
    val preferLocalForPrivateFiles: Boolean = true
) 