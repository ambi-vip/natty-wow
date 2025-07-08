package site.weixing.natty.api.common.filestorage.file

/**
 * 文件状态枚举
 */
enum class FileStatus {
    /**
     * 上传中
     */
    UPLOADING,
    
    /**
     * 正常状态
     */
    ACTIVE,
    
    /**
     * 已删除状态
     */
    DELETED,
    
    /**
     * 已归档状态
     */
    ARCHIVED,
    
    /**
     * 损坏状态
     */
    CORRUPTED
} 