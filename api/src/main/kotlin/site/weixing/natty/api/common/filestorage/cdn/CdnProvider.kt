package site.weixing.natty.api.common.filestorage.cdn

/**
 * CDN提供商枚举
 */
enum class CdnProvider(
    val code: String,
    val displayName: String,
    val description: String
) {
    CLOUDFLARE("cloudflare", "Cloudflare CDN", "使用Cloudflare CDN服务"),
    ALIYUN_CDN("aliyun_cdn", "阿里云CDN", "使用阿里云CDN服务"),
    TENCENT_CDN("tencent_cdn", "腾讯云CDN", "使用腾讯云CDN服务"),
    QINIU_CDN("qiniu_cdn", "七牛云CDN", "使用七牛云CDN服务"),
    CUSTOM("custom", "自定义CDN", "使用自定义CDN配置");
    
    companion object {
        fun fromCode(code: String): CdnProvider? {
            return values().find { it.code == code }
        }
    }
} 