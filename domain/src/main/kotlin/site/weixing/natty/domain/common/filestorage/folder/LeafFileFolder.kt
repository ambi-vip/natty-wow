package site.weixing.natty.domain.common.filestorage.folder

import me.ahoo.wow.models.tree.Leaf
import me.ahoo.wow.models.tree.ROOT_CODE
import site.weixing.natty.api.common.filestorage.folder.FileFolderStatus

/**
 * 叶子文件夹类
 * 用于树形结构的叶子节点表示
 */
data class LeafFileFolder(
    override val name: String,
    override val code: String,
    override val sortId: Int,
    val status: FileFolderStatus = FileFolderStatus.ACTIVE,
    val description: String? = null,
    val permissions: Map<String, Set<String>> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val fileCount: Long = 0,
    val totalSize: Long = 0,
    override val children: List<LeafFileFolder> = emptyList()
) : Leaf<LeafFileFolder> {

    override fun withChildren(children: List<LeafFileFolder>): LeafFileFolder {
        return copy(children = children)
    }
    
    /**
     * 是否为根文件夹
     */
    override fun isRoot(): Boolean {
        return code == ROOT_CODE
    }
    
    /**
     * 是否有子文件夹
     */
    fun hasChildren(): Boolean {
        return children.isNotEmpty()
    }
    
    /**
     * 获取子文件夹数量
     */
    fun getChildrenCount(): Int {
        return children.size
    }
    
    /**
     * 获取总文件数量（包括子文件夹）
     */
    fun getTotalFileCount(): Long {
        return fileCount + children.sumOf { it.getTotalFileCount() }
    }


    /**
     * 获取总大小（包括子文件夹）
     */
    fun totalSizeIncludingChildren(): Long {
        return totalSize + children.sumOf { it.totalSizeIncludingChildren() }
    }
    
    /**
     * 查找子文件夹
     */
    fun findChild(code: String): LeafFileFolder? {
        return children.find { it.code == code }
    }
    
    /**
     * 查找子文件夹（递归）
     */
    fun findDescendant(code: String): LeafFileFolder? {
        children.forEach { child ->
            if (child.code == code) {
                return child
            }
            child.findDescendant(code)?.let { return it }
        }
        return null
    }
    
    companion object {
        /**
         * 根文件夹
         */
        val ROOT = LeafFileFolder(
            name = ROOT_CODE,
            code = ROOT_CODE,
            sortId = 0,
            status = FileFolderStatus.ACTIVE,
            description = "根文件夹",
            children = emptyList()
        )
    }
} 