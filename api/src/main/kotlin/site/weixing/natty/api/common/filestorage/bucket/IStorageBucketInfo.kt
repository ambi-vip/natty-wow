package site.weixing.natty.api.common.filestorage.bucket

import java.time.LocalDateTime

interface IStorageBucketInfo {
    val bucketName: String?
    val ownerId: String?
    val maxCapacity: Long
    val usedCapacity: Long
    val maxFileSize: Long
    val allowedTypes: Set<String>
    val isPublic: Boolean
    val enableCompression: Boolean
    val enableEncryption: Boolean
    val retentionDays: Int
    val status: BucketStatus
    val description: String?
    
    // 统计信息
    val fileCount: Long
    val lastAccessTime: LocalDateTime?
    val lastCleanupTime: LocalDateTime?
    

}