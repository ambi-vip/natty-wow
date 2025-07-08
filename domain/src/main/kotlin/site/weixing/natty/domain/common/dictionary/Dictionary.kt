package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.api.command.DefaultDeleteAggregate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.dictionary.AddDictionaryItem
import site.weixing.natty.api.common.dictionary.ChangeDictionaryItemStatus
import site.weixing.natty.api.common.dictionary.ChangeDictionaryStatus
import site.weixing.natty.api.common.dictionary.CreateDictionary
import site.weixing.natty.api.common.dictionary.DictionaryCreated
import site.weixing.natty.api.common.dictionary.DictionaryDeleted
import site.weixing.natty.api.common.dictionary.DictionaryItemAdded
import site.weixing.natty.api.common.dictionary.DictionaryItemRemoved
import site.weixing.natty.api.common.dictionary.DictionaryItemStatus
import site.weixing.natty.api.common.dictionary.DictionaryItemStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryItemUpdated
import site.weixing.natty.api.common.dictionary.DictionaryStatus
import site.weixing.natty.api.common.dictionary.DictionaryStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryUpdated
import site.weixing.natty.api.common.dictionary.RemoveDictionaryItem
import site.weixing.natty.api.common.dictionary.UpdateDictionary
import site.weixing.natty.api.common.dictionary.UpdateDictionaryItem

/**
 * 字典聚合根
 *
 * @param state 字典状态
 */
@Suppress("unused")
@AggregateRoot
@StaticTenantId
class Dictionary(private val state: DictionaryState) {

    /**
     * 处理创建字典命令
     *
     * @param command 创建字典命令
     * @return 字典创建事件
     */
    @OnCommand
    fun onCreate(
        command: CreateDictionary,
        dictionaryPrepares: DictionaryPrepares
    ): Mono<DictionaryCreated> {
        return dictionaryPrepares.usingPrepare(
            key = command.code,
            value = state.code,
        ) {
            require(it) {
                "code[${command.code}] is already registered."
            }
            DictionaryCreated(
                dictionaryId = state.id,
                code = command.code,
                name = command.name,
                description = command.description
            ).toMono()
        }
    }

    /**
     * 处理更新字典命令
     *
     * @param command 更新字典命令
     * @return 字典更新事件
     */
    @OnCommand
    fun onUpdate(command: UpdateDictionary): DictionaryUpdated {
        require(state.status == DictionaryStatus.ACTIVE) {
            "字典[${command.id}]状态不允许更新。"
        }
        return DictionaryUpdated(
            dictionaryId = command.id,
            name = command.name,
            description = command.description
        )
    }

    /**
     * 处理改变字典状态命令
     *
     * @param command 改变字典状态命令
     * @return 字典状态改变事件
     */
    @OnCommand
    fun onChangeStatus(command: ChangeDictionaryStatus): DictionaryStatusChanged {
        require(state.status != command.status) {
            "字典[${command.id}]已处于目标状态[${command.status.name}]。"
        }
        return DictionaryStatusChanged(
            dictionaryId = command.id,
            status = command.status
        )
    }

    /**
     * 处理删除字典命令
     *
     * @param command 删除字典命令
     * @return 字典删除事件
     */
    @OnCommand
    fun onDelete(
        command: DefaultDeleteAggregate,
        dictionaryPrepares: DictionaryPrepares,
    ): Mono<DictionaryDeleted> {
        return dictionaryPrepares.rollback(state.code)
            .map {
                DictionaryDeleted(
                    dictionaryId = state.id,
                    code = state.code,
                    name = state.name ?: ""
                )
            }
    }

    /**
     * 处理添加字典项命令
     *
     * @param command 添加字典项命令
     * @return 字典项添加事件
     */
    @OnCommand
    fun onAddItem(command: AddDictionaryItem): DictionaryItemAdded {
        require(state.status == DictionaryStatus.ACTIVE) {
            "字典[${state.code}]状态不允许添加字典项。"
        }
        require(!state.hasItem(command.itemCode)) {
            "字典项编码[${command.itemCode}]已存在。"
        }
        
        val itemValue = command.itemValue?.ifEmpty { command.itemCode } ?: command.itemCode
        
        return DictionaryItemAdded(
            dictionaryId = state.id,
            itemCode = command.itemCode,
            itemName = command.itemName,
            itemValue = itemValue,
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
    fun onUpdateItem(command: UpdateDictionaryItem): DictionaryItemUpdated {
        require(state.status == DictionaryStatus.ACTIVE) {
            "字典[${state.code}]状态不允许更新字典项。"
        }
        
        val existingItem = state.getItem(command.itemCode)
        require(existingItem != null) {
            "字典项[${command.itemCode}]不存在。"
        }
        require(existingItem.status == DictionaryItemStatus.ACTIVE) {
            "字典项[${command.itemCode}]状态不允许更新。"
        }
        
        val itemValue = command.itemValue?.ifEmpty { command.itemCode } ?: command.itemCode
        
        return DictionaryItemUpdated(
            dictionaryId = state.id,
            itemCode = command.itemCode,
            itemName = command.itemName,
            itemValue = itemValue,
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
    fun onChangeItemStatus(command: ChangeDictionaryItemStatus): DictionaryItemStatusChanged {
        val existingItem = state.getItem(command.itemCode)
        require(existingItem != null) {
            "字典项[${command.itemCode}]不存在。"
        }
        require(existingItem.status != command.status) {
            "字典项[${command.itemCode}]已处于目标状态[${command.status.name}]。"
        }
        
        return DictionaryItemStatusChanged(
            dictionaryId = state.id,
            itemCode = command.itemCode,
            status = command.status
        )
    }

    /**
     * 处理移除字典项命令
     *
     * @param command 移除字典项命令
     * @return 字典项移除事件
     */
    @OnCommand
    fun onRemoveItem(command: RemoveDictionaryItem): DictionaryItemRemoved {
        require(state.hasItem(command.itemCode)) {
            "字典项[${command.itemCode}]不存在。"
        }
        
        return DictionaryItemRemoved(
            dictionaryId = state.id,
            itemCode = command.itemCode
        )
    }
}
