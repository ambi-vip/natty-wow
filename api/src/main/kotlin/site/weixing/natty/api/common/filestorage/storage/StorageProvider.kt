package site.weixing.natty.api.common.filestorage.storage

/**
 * 存储提供商枚举
 */
enum class StorageProvider(
    val code: String,
    val displayName: String,
    val description: String
) {
    LOCAL("local", "本地存储", "使用本地文件系统存储文件"),
    S3("s3", "Amazon S3", "使用Amazon S3对象存储服务"),
    ALIYUN_OSS("aliyun_oss", "阿里云OSS", "使用阿里云对象存储服务");
    
    companion object {
        fun fromCode(code: String): StorageProvider? {
            return entries.find { it.code == code }
        }
    }
} 