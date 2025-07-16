package site.weixing.natty.api.common.filestorage.folder

/**
 * 文件夹状态枚举
 */
enum class FileFolderStatus {
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
    ARCHIVED
} 