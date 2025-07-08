package site.weixing.natty.domain.common.filestorage.folder

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.models.tree.aggregate.Tree
import site.weixing.natty.api.common.filestorage.folder.CreateFileFolder
import site.weixing.natty.api.common.filestorage.folder.UpdateFileFolder
import site.weixing.natty.api.common.filestorage.folder.DeleteFileFolder
import site.weixing.natty.api.common.filestorage.folder.MoveFileFolder

/**
 * 文件夹聚合根
 * 继承Tree基类，提供标准的树形结构管理能力
 * @author system
 */
@AggregateRoot
@StaticTenantId
class FileFolder(state: FileFolderState) :
    Tree<FileFolderState, CreateFileFolder, UpdateFileFolder, DeleteFileFolder, MoveFileFolder>(state) {

    override fun generateCode(): String {
        return GlobalIdGenerator.generateAsString()
    }

    override fun maxLevel(): Int {
        return 20  // 文件夹最大层级深度为20级
    }
    
    /**
     * 验证文件夹名称是否符合规范
     */
    private fun validateFolderName(name: String): Boolean {
        // 文件夹名称不能包含特殊字符
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return name.none { it in invalidChars } && name.trim() == name
    }
    
    /**
     * 验证文件夹权限配置是否有效
     */
    private fun validatePermissions(permissions: Map<String, Set<String>>): Boolean {
        val validActions = setOf("read", "write", "delete", "share", "admin")
        return permissions.values.all { actions ->
            actions.all { it in validActions }
        }
    }
} 