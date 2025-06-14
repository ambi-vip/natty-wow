package site.weixing.natty.domain.common.item

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import site.weixing.natty.api.common.item.ChangeDictionaryItemStatus
import site.weixing.natty.api.common.item.CreateDictionaryItem
import site.weixing.natty.api.common.item.DeleteDictionaryItem
import site.weixing.natty.api.common.item.DictionaryItemCreated
import site.weixing.natty.api.common.item.DictionaryItemDeleted
import site.weixing.natty.api.common.item.DictionaryItemStatusChanged
import site.weixing.natty.api.common.item.DictionaryItemUpdated
import site.weixing.natty.api.common.item.UpdateDictionaryItem
import site.weixing.natty.domain.common.item.DictionaryItemState.DictionaryItemStatus

/**
 * 字典项聚合根
 *
 * @param state 字典项状态
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class DictionaryItem(private val state: DictionaryItemState) {

    /**
     * 处理创建字典项命令
     *
     * @param command 创建字典项命令
     * @return 字典项创建事件
     */
    @OnCommand
    fun onCreate(command: CreateDictionaryItem): DictionaryItemCreated {
        require(state.status == DictionaryItemStatus.ACTIVE) {
            "字典项[${command.itemCode}]已存在或状态不允许创建。"
        }
        return DictionaryItemCreated(
            dictionaryItemId = state.id,
            dictionaryId = command.dictionaryId,
            dictionaryCode = command.dictionaryCode,
            itemCode = command.itemCode,
            itemName = command.itemName,
            itemValue = command.itemValue,
            sortOrder = command.sortOrder,
            description = command.description,
            localizedNames = command.localizedNames
        )
    }

    /**
     * 处理更新字典项命令
     *
     * @param command 更新字典项命令
     * @return 字典项更新事件
     */
    @OnCommand
    fun onUpdate(command: UpdateDictionaryItem): DictionaryItemUpdated {
        require(state.status == DictionaryItemStatus.ACTIVE) {
            "字典项[${command.id}]状态不允许更新。"
        }
        return DictionaryItemUpdated(
            dictionaryItemId = command.id,
            itemName = command.itemName,
            itemValue = command.itemValue,
            sortOrder = command.sortOrder,
            description = command.description,
            localizedNames = command.localizedNames
        )
    }

    /**
     * 处理改变字典项状态命令
     *
     * @param command 改变字典项状态命令
     * @return 字典项状态改变事件
     */
    @OnCommand
    fun onChangeStatus(command: ChangeDictionaryItemStatus): DictionaryItemStatusChanged {
        val newStatus = DictionaryItemStatus.valueOf(command.status)
        require(state.status != newStatus) {
            "字典项[${command.id}]已处于目标状态[${newStatus.name}]。"
        }
        return DictionaryItemStatusChanged(
            dictionaryItemId = command.id,
            status = command.status
        )
    }

    /**
     * 处理删除字典项命令
     *
     * @param command 删除字典项命令
     * @return 字典项删除事件
     */
    @OnCommand
    fun onDelete(command: DeleteDictionaryItem): DictionaryItemDeleted {
        require(state.status != DictionaryItemStatus.DELETED) {
            "字典项[${command.id}]已删除。"
        }
        return DictionaryItemDeleted(
            dictionaryItemId = command.id,
            dictionaryCode = state.dictionaryCode!!
        )
    }
} 