package site.weixing.natty.api.common.filestorage.config

import jakarta.validation.constraints.NotBlank

/**
 * 阿里云OSS存储配置参数
 * 预留接口，暂不实现具体功能
 */
data class AliyunOssStorageParams(
    @field:NotBlank(message = "访问密钥ID不能为空")
    val accessKeyId: String,
    
    @field:NotBlank(message = "访问密钥不能为空")
    val accessKeySecret: String,
    
    @field:NotBlank(message = "存储桶名称不能为空")
    val bucketName: String,
    
    @field:NotBlank(message = "端点地址不能为空")
    val endpoint: String,
    
    val storageClass: String = "Standard",
    
    val enableServerSideEncryption: Boolean = true,
    
    val enableCrc: Boolean = true,
    
    val connectTimeout: Int = 50000,
    
    val socketTimeout: Int = 50000
) {
    companion object {
        fun fromMap(params: Map<String, Any>): AliyunOssStorageParams {
            return AliyunOssStorageParams(
                accessKeyId = params["accessKeyId"] as? String ?: "",
                accessKeySecret = params["accessKeySecret"] as? String ?: "",
                bucketName = params["bucketName"] as? String ?: "",
                endpoint = params["endpoint"] as? String ?: "",
                storageClass = params["storageClass"] as? String ?: "Standard",
                enableServerSideEncryption = params["enableServerSideEncryption"] as? Boolean ?: true,
                enableCrc = params["enableCrc"] as? Boolean ?: true,
                connectTimeout = (params["connectTimeout"] as? Number)?.toInt() ?: 50000,
                socketTimeout = (params["socketTimeout"] as? Number)?.toInt() ?: 50000
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "accessKeyId" to accessKeyId,
            "accessKeySecret" to accessKeySecret,
            "bucketName" to bucketName,
            "endpoint" to endpoint,
            "storageClass" to storageClass,
            "enableServerSideEncryption" to enableServerSideEncryption,
            "enableCrc" to enableCrc,
            "connectTimeout" to connectTimeout,
            "socketTimeout" to socketTimeout
        )
    }
} 