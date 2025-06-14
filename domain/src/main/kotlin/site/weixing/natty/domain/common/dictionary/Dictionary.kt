package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.api.common.dictionary.ChangeDictionaryStatus
import site.weixing.natty.api.common.dictionary.CreateDictionary
import site.weixing.natty.api.common.dictionary.DeleteDictionary
import site.weixing.natty.api.common.dictionary.DictionaryCreated
import site.weixing.natty.api.common.dictionary.DictionaryDeleted
import site.weixing.natty.api.common.dictionary.DictionaryStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryUpdated
import site.weixing.natty.api.common.dictionary.UpdateDictionary
import site.weixing.natty.domain.common.dictionary.DictionaryState.DictionaryStatus

/**
 * 字典聚合根
 *
 * @param state 字典状态
 */
@Suppress("unused")
@AggregateRoot
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
        dictionaryPrepare: DictionaryPrepare
    ): Mono<DictionaryCreated> {

        require(state.status == DictionaryStatus.ACTIVE) {
            "字典[${command.code}]已存在或状态不允许创建。"
        }

        return dictionaryPrepare.usingPrepare(
            key = command.code,
            value = state.id,
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
        val newStatus = DictionaryStatus.valueOf(command.status)
        require(state.status != newStatus) {
            "字典[${command.id}]已处于目标状态[${newStatus.name}]。"
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
    fun onDelete(command: DeleteDictionary): DictionaryDeleted {
        require(state.status != DictionaryStatus.DELETED) {
            "字典[${command.id}]已删除。"
        }
        return DictionaryDeleted(
            dictionaryId = command.id
        )
    }
} 