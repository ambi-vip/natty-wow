package site.weixing.natty.domain.common.filestorage.folder

import me.ahoo.wow.models.tree.CopySortIdFlat
import site.weixing.natty.api.common.filestorage.folder.FileFolderStatus

/**
 * 扁平化文件夹类
 * 用于树形结构的扁平化表示
 */
data class FlatFileFolder(
    override val name: String,
    override val code: String,
    override val sortId: Int,
    val status: FileFolderStatus = FileFolderStatus.ACTIVE,
    val description: String? = null,
    val permissions: Map<String, Set<String>> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val fileCount: Long = 0,
    val totalSize: Long = 0
) : CopySortIdFlat<FlatFileFolder> {
    
    override fun withSortId(sortId: Int): FlatFileFolder {
        return copy(sortId = sortId)
    }

    /**
     * 转换为叶子文件夹
     */
    fun toLeaf(): LeafFileFolder {
        return LeafFileFolder(
            name = name,
            code = code,
            sortId = sortId,
            status = status,
            description = description,
            permissions = permissions,
            metadata = metadata,
            fileCount = fileCount,
            totalSize = totalSize,
            children = emptyList()
        )
    }
    
    /**
     * 是否为根文件夹
     */
    override fun isRoot(): Boolean {
        return code == "ROOT"
    }
    
    /**
     * 获取文件夹路径
     */
    fun getPath(): String {
        return if (isRoot()) "/" else "/$name"
    }
} 