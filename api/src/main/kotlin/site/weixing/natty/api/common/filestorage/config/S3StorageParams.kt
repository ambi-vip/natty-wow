package site.weixing.natty.api.common.filestorage.config

import jakarta.validation.constraints.NotBlank

/**
 * S3存储配置参数
 * 预留接口，暂不实现具体功能
 */
data class S3StorageParams(
    @field:NotBlank(message = "访问密钥ID不能为空")
    val accessKeyId: String,
    
    @field:NotBlank(message = "访问密钥不能为空")
    val secretAccessKey: String,
    
    @field:NotBlank(message = "存储桶名称不能为空")
    val bucketName: String,
    
    @field:NotBlank(message = "区域不能为空")
    val region: String,
    
    val endpoint: String? = null,
    
    val pathStyleAccess: Boolean = false,
    
    val enableServerSideEncryption: Boolean = true,
    
    val storageClass: String = "STANDARD"
) {
    companion object {
        fun fromMap(params: Map<String, Any>): S3StorageParams {
            return S3StorageParams(
                accessKeyId = params["accessKeyId"] as? String ?: "",
                secretAccessKey = params["secretAccessKey"] as? String ?: "",
                bucketName = params["bucketName"] as? String ?: "",
                region = params["region"] as? String ?: "",
                endpoint = params["endpoint"] as? String,
                pathStyleAccess = params["pathStyleAccess"] as? Boolean ?: false,
                enableServerSideEncryption = params["enableServerSideEncryption"] as? Boolean ?: true,
                storageClass = params["storageClass"] as? String ?: "STANDARD"
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "accessKeyId" to accessKeyId,
            "secretAccessKey" to secretAccessKey,
            "bucketName" to bucketName,
            "region" to region,
            "endpoint" to (endpoint ?: ""),
            "pathStyleAccess" to pathStyleAccess,
            "enableServerSideEncryption" to enableServerSideEncryption,
            "storageClass" to storageClass
        )
    }
} 