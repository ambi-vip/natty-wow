package site.weixing.natty.api.common.filestorage.bucket

/**
 * 存储桶状态枚举
 */
enum class BucketStatus {
    /**
     * 活动状态 - 正常使用
     */
    ACTIVE,
    
    /**
     * 暂停状态 - 暂时停用，不能上传新文件，但可以访问现有文件
     */
    SUSPENDED,
    
    /**
     * 归档状态 - 长期存储，访问性能较低
     */
    ARCHIVED,
    
    /**
     * 已删除状态 - 标记删除，等待物理清理
     */
    DELETED
}