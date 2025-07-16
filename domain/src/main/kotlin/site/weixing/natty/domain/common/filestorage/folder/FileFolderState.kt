package site.weixing.natty.domain.common.filestorage.folder

import me.ahoo.wow.models.tree.Flat
import me.ahoo.wow.models.tree.aggregate.TreeState
import site.weixing.natty.api.common.filestorage.folder.FileFolderCreated
import site.weixing.natty.api.common.filestorage.folder.FileFolderUpdated
import site.weixing.natty.api.common.filestorage.folder.FileFolderDeleted
import site.weixing.natty.api.common.filestorage.folder.FileFolderMoved
import site.weixing.natty.api.common.filestorage.folder.FileFolderStatus

/**
 * 文件夹状态类
 * 继承TreeState，管理文件夹的树形结构状态
 */
class FileFolderState(override val id: String) :
    TreeState<FlatFileFolder, FileFolderCreated, FileFolderUpdated, FileFolderDeleted, FileFolderMoved>() {

    var status: FileFolderStatus = FileFolderStatus.ACTIVE
        private set
    
    var description: String? = null
        private set
    
    var permissions: Map<String, Set<String>> = emptyMap()
        private set
    
    var metadata: Map<String, String> = emptyMap()
        private set
    
    var fileCount: Long = 0
        private set
    
    var totalSize: Long = 0
        private set

    override fun Flat.toFlat(): FlatFileFolder {
        return FlatFileFolder(
            name = name,
            code = code,
            sortId = sortId,
            status = this@FileFolderState.status,
            description = this@FileFolderState.description,
            permissions = this@FileFolderState.permissions,
            metadata = this@FileFolderState.metadata,
            fileCount = this@FileFolderState.fileCount,
            totalSize = this@FileFolderState.totalSize
        )
    }
    
    fun updateDescription(newDescription: String?) {
        this.description = newDescription
    }
    
    fun updatePermissions(newPermissions: Map<String, Set<String>>) {
        this.permissions = newPermissions
    }
    
    fun updateMetadata(newMetadata: Map<String, String>) {
        this.metadata = newMetadata
    }
    
    fun incrementFileCount() {
        this.fileCount++
    }
    
    fun decrementFileCount() {
        if (this.fileCount > 0) {
            this.fileCount--
        }
    }
    
    fun addFileSize(size: Long) {
        this.totalSize += size
    }
    
    fun subtractFileSize(size: Long) {
        if (this.totalSize >= size) {
            this.totalSize -= size
        }
    }
    
    fun changeStatus(newStatus: FileFolderStatus) {
        this.status = newStatus
    }
} 