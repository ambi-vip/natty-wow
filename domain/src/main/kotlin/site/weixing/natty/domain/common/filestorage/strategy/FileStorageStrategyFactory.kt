package site.weixing.natty.domain.common.filestorage.strategy

import site.weixing.natty.api.common.filestorage.storage.StorageProvider
import site.weixing.natty.domain.common.filestorage.strategy.impl.LocalFileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.impl.S3FileStorageStrategy
import site.weixing.natty.domain.common.filestorage.strategy.impl.AliyunOssFileStorageStrategy
import site.weixing.natty.domain.common.filestorage.exception.StorageConfigurationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 文件存储策略工厂
 * 根据存储提供商和配置参数创建对应的存储策略实例
 */
@Service
class FileStorageStrategyFactory {

    companion object {
        private val logger = LoggerFactory.getLogger(FileStorageStrategyFactory::class.java)
    }

    /**
     * 创建存储策略实例
     * @param provider 存储提供商
     * @param config 配置参数映射
     * @return 存储策略实例
     */
    fun createStrategy(
        provider: StorageProvider,
        id: String,
        config: Map<String, Any>
    ): FileStorageStrategy {
        logger.info("创建存储策略: provider={}, config keys={}", provider, config.keys)

        return when (provider) {
            StorageProvider.LOCAL -> createLocalStrategy(id, config)
            StorageProvider.S3 -> createS3Strategy(id, config)
            StorageProvider.ALIYUN_OSS -> createALiYunOssStrategy(id, config)
        }
    }

    /**
     * 创建本地存储策略
     */
    private fun createLocalStrategy(id: String, config: Map<String, Any>): LocalFileStorageStrategy {
        try {
            val baseDirectory = getRequiredStringParam(config, "baseDirectory", "本地存储基础目录")
            val maxFileSize = getOptionalLongParam(config, "maxFileSize", 100 * 1024 * 1024L) // 默认100MB
            val allowedContentTypes = getOptionalStringListParam(config, "allowedContentTypes", emptyList())
            val enableChecksumValidation = getOptionalBooleanParam(config, "enableChecksumValidation", true)
            val urlPrefix = getOptionalStringParam(config, "urlPrefix", "file://")

            return LocalFileStorageStrategy(
                id = id,
                baseDirectory = baseDirectory,
                maxFileSize = maxFileSize,
                allowedContentTypes = allowedContentTypes.toSet(),
                enableChecksumValidation = enableChecksumValidation,
                urlPrefix = urlPrefix
            )
        } catch (e: Exception) {
            throw StorageConfigurationException("LOCAL", "本地存储配置错误", e)
        }
    }

    /**
     * 创建S3存储策略
     */
    private fun createS3Strategy(id : String,config: Map<String, Any>): S3FileStorageStrategy {
        try {
            val accessKeyId = getRequiredStringParam(config, "accessKeyId", "S3访问密钥ID")
            val secretAccessKey = getRequiredStringParam(config, "secretAccessKey", "S3访问密钥")
            val region = getRequiredStringParam(config, "region", "S3区域")
            val bucketName = getRequiredStringParam(config, "bucketName", "S3存储桶名称")
            val endpointUrl = getOptionalStringParam(config, "endpointUrl")

            return S3FileStorageStrategy(
                id = id,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = region,
                bucketName = bucketName,
                endpointUrl = endpointUrl
            )
        } catch (e: Exception) {
            throw StorageConfigurationException("S3", "S3存储配置错误", e)
        }
    }

    /**
     * 创建阿里云OSS存储策略
     */
    private fun createALiYunOssStrategy(id: String, config: Map<String, Any>): AliyunOssFileStorageStrategy {
        try {
            val accessKeyId = getRequiredStringParam(config, "accessKeyId", "阿里云OSS访问密钥ID")
            val accessKeySecret = getRequiredStringParam(config, "accessKeySecret", "阿里云OSS访问密钥")
            val endpoint = getRequiredStringParam(config, "endpoint", "阿里云OSS端点")
            val bucketName = getRequiredStringParam(config, "bucketName", "阿里云OSS存储桶名称")
            val region = getOptionalStringParam(config, "region")

            return AliyunOssFileStorageStrategy(
                id = id,
                accessKeyId = accessKeyId,
                accessKeySecret = accessKeySecret,
                endpoint = endpoint,
                bucketName = bucketName,
                region = region
            )
        } catch (e: Exception) {
            throw StorageConfigurationException("ALIYUN_OSS", "阿里云OSS存储配置错误", e)
        }
    }



    /**
     * 验证存储配置参数的完整性
     * @param provider 存储提供商
     * @param config 配置参数
     * @return 验证结果，包含错误信息
     */
    fun validateConfig(
        provider: StorageProvider,
        config: Map<String, Any>
    ): ConfigValidationResult {
        val errors = mutableListOf<String>()

        try {
            when (provider) {
                StorageProvider.LOCAL -> validateLocalConfig(config, errors)
                StorageProvider.S3 -> validateS3Config(config, errors)
                StorageProvider.ALIYUN_OSS -> validateAliyunOssConfig(config, errors)
            }
        } catch (e: Exception) {
            errors.add("配置验证过程中发生异常: ${e.message}")
        }

        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * 验证本地存储配置
     */
    private fun validateLocalConfig(config: Map<String, Any>, errors: MutableList<String>) {
        validateRequiredParam(config, "baseDirectory", "本地存储基础目录", errors)
        
        val maxFileSize = config["maxFileSize"]
        if (maxFileSize != null && maxFileSize !is Number) {
            errors.add("maxFileSize必须是数字类型")
        }

        val enableChecksumValidation = config["enableChecksumValidation"]
        if (enableChecksumValidation != null && enableChecksumValidation !is Boolean) {
            errors.add("enableChecksumValidation必须是布尔类型")
        }
    }

    /**
     * 验证S3存储配置
     */
    private fun validateS3Config(config: Map<String, Any>, errors: MutableList<String>) {
        validateRequiredParam(config, "accessKeyId", "S3访问密钥ID", errors)
        validateRequiredParam(config, "secretAccessKey", "S3访问密钥", errors)
        validateRequiredParam(config, "region", "S3区域", errors)
        validateRequiredParam(config, "bucketName", "S3存储桶名称", errors)
    }

    /**
     * 验证阿里云OSS存储配置
     */
    private fun validateAliyunOssConfig(config: Map<String, Any>, errors: MutableList<String>) {
        validateRequiredParam(config, "accessKeyId", "阿里云OSS访问密钥ID", errors)
        validateRequiredParam(config, "accessKeySecret", "阿里云OSS访问密钥", errors)
        validateRequiredParam(config, "endpoint", "阿里云OSS端点", errors)
        validateRequiredParam(config, "bucketName", "阿里云OSS存储桶名称", errors)
    }

    // 私有辅助方法

    private fun getRequiredStringParam(
        config: Map<String, Any>,
        key: String,
        description: String
    ): String {
        val value = config[key] as? String
        require(!value.isNullOrBlank()) { "$description ($key) 不能为空" }
        return value
    }

    private fun getOptionalStringParam(
        config: Map<String, Any>,
        key: String,
        defaultValue: String = "file://" // 使用空字符串作为默认的默认值
    ): String {
        return (config[key] as? String)?.takeIf { it.isNotBlank() } ?: defaultValue
    }

    private fun getOptionalLongParam(
        config: Map<String, Any>,
        key: String,
        defaultValue: Long
    ): Long {
        return when (val value = config[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    private fun getOptionalBooleanParam(
        config: Map<String, Any>,
        key: String,
        defaultValue: Boolean
    ): Boolean {
        return when (val value = config[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOptionalStringListParam(
        config: Map<String, Any>,
        key: String,
        defaultValue: List<String>
    ): List<String> {
        return when (val value = config[key]) {
            is List<*> -> {
                try {
                    value.filterIsInstance<String>()
                } catch (e: Exception) {
                    defaultValue
                }
            }
            is String -> listOf(value)
            else -> defaultValue
        }
    }

    private fun validateRequiredParam(
        config: Map<String, Any>,
        key: String,
        description: String,
        errors: MutableList<String>
    ) {
        val value = config[key] as? String
        if (value.isNullOrBlank()) {
            errors.add("$description ($key) 不能为空")
        }
    }
}

/**
 * 配置验证结果
 */
data class ConfigValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    /**
     * 获取错误信息的字符串表示
     */
    fun getErrorMessage(): String {
        return errors.joinToString("\n") { "- $it" }
    }
} 