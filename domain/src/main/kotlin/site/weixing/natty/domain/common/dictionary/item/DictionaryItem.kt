package site.weixing.natty.domain.common.dictionary.item

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.api.command.DefaultDeleteAggregate
import me.ahoo.wow.exception.throwNotFoundIfNull
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.toState
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.dictionary.item.ChangeDictionaryItemStatus
import site.weixing.natty.api.common.dictionary.item.CreateDictionaryItem
import site.weixing.natty.api.common.dictionary.item.DictionaryItemCreated
import site.weixing.natty.api.common.dictionary.item.DictionaryItemDeleted
import site.weixing.natty.api.common.dictionary.item.DictionaryItemStatusChanged
import site.weixing.natty.api.common.dictionary.item.DictionaryItemUpdated
import site.weixing.natty.api.common.dictionary.item.UpdateDictionaryItem
import site.weixing.natty.domain.common.dictionary.DictionaryPrepares
import site.weixing.natty.domain.common.dictionary.DictionaryState
import site.weixing.natty.domain.common.dictionary.item.DictionaryItemState.DictionaryItemStatus

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
    fun onCreate(
        command: CreateDictionaryItem,
        dictQueryService: SnapshotQueryService<DictionaryState>
    ): Mono<DictionaryItemCreated> {
        return DictionaryPrepares.ITEM_CODE.usingPrepare(
            key = command.itemCode,
            value = state.itemCode,
        ) {
            require(it) {
                "code[${command.itemCode}] is already registered."
            }
            dictQueryService.single(
                singleQuery {
                    condition {
                        nestedState()
                        id((command.dictionaryId))
                    }
                }
            ).toState().throwNotFoundIfNull("字典不存在")
                .flatMap { dictState ->
                    DictionaryItemCreated(
                        dictionaryItemId = state.id,
                        dictionaryId = command.dictionaryId,
                        dictionaryCode = dictState.code,
                        itemCode = command.itemCode,
                        itemName = command.itemName,
                        itemValue = command.itemValue?.ifEmpty { command.itemCode } ?: command.itemCode,
                        sortOrder = command.sortOrder,
                        description = command.description,
                        localizedNames = command.localizedNames
                    ).toMono()
                }
        }
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
    fun onDelete(command: DefaultDeleteAggregate): Mono<DictionaryItemDeleted> {
        return DictionaryPrepares.ITEM_CODE.rollback(state.itemCode)
            .map {
                DictionaryItemDeleted(
                    dictionaryItemId = state.id,
                    dictionaryCode = state.dictionaryCode
                )
            }
    }
}
