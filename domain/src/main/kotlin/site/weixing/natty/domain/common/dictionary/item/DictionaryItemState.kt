package site.weixing.natty.domain.common.dictionary.item

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.dictionary.item.DictionaryItemCreated
import site.weixing.natty.api.common.dictionary.item.DictionaryItemStatusChanged
import site.weixing.natty.api.common.dictionary.item.DictionaryItemUpdated
/**
 * 字典项状态
 *
 * @param id 字典项ID
 * @param dictionaryId 所属字典的ID
 * @param itemCode 字典项编码
 * @param itemName 字典项名称
 * @param itemValue 字典项值
 * @param sortOrder 排序
 * @param description 字典项描述
 * @param status 字典项状态
 * @param localizedNames 多语言名称
 */
data class DictionaryItemState(override val id: String) : Identifier {

    var dictionaryId: String = ""
        private set
    var dictionaryCode: String = ""
        private set
    var itemCode: String = ""
        private set
    var itemName: String? = null
        private set
    var itemValue: String? = null
        private set
    var sortOrder: Int? = null
        private set
    var description: String? = null
        private set
    var status: DictionaryItemStatus = DictionaryItemStatus.ACTIVE
        private set
    var localizedNames: Map<String, String>? = null
        private set

    /**
     * 应用字典项创建事件
     *
     * @param event 字典项创建事件
     */
    @OnSourcing
    fun onCreated(event: DictionaryItemCreated) {
        this.dictionaryId = event.dictionaryId
        this.dictionaryCode = event.dictionaryCode
        this.itemCode = event.itemCode
        this.itemName = event.itemName
        this.itemValue = event.itemValue
        this.sortOrder = event.sortOrder
        this.description = event.description
        this.localizedNames = event.localizedNames
        this.status = DictionaryItemStatus.ACTIVE
    }

    /**
     * 应用字典项更新事件
     *
     * @param event 字典项更新事件
     */
    @OnSourcing
    fun onUpdated(event: DictionaryItemUpdated) {
        this.itemName = event.itemName
        this.itemValue = event.itemValue
        this.sortOrder = event.sortOrder
        this.description = event.description
        this.localizedNames = event.localizedNames
    }

    /**
     * 应用字典项状态改变事件
     *
     * @param event 字典项状态改变事件
     */
    @OnSourcing
    fun onStatusChanged(event: DictionaryItemStatusChanged) {
        this.status = DictionaryItemStatus.valueOf(event.status)
    }

    /**
     * 字典项状态枚举
     */
    enum class DictionaryItemStatus {
        /** 启用 */
        ACTIVE,

        /** 禁用 */
        INACTIVE,

        /** 删除 */
        DELETED
    }
}
