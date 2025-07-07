package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.dictionary.DictionaryCreated
import site.weixing.natty.api.common.dictionary.DictionaryItemAdded
import site.weixing.natty.api.common.dictionary.DictionaryItemRemoved
import site.weixing.natty.api.common.dictionary.DictionaryItemStatus
import site.weixing.natty.api.common.dictionary.DictionaryItemStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryItemUpdated
import site.weixing.natty.api.common.dictionary.DictionaryStatus
import site.weixing.natty.api.common.dictionary.DictionaryStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryUpdated

/**
 * 字典状态
 *
 * @param id 字典ID
 */
data class DictionaryState(override val id: String) : Identifier {

    var code: String = ""
        private set
    var name: String? = null
        private set
    var description: String? = null
        private set
    var status: DictionaryStatus = DictionaryStatus.ACTIVE
        private set
    
    private val items: MutableMap<String, DictionaryItem> = mutableMapOf()

    /**
     * 获取字典项集合
     */
    fun getItems(): Map<String, DictionaryItem> = items.toMap()

    /**
     * 根据编码获取字典项
     */
    fun getItem(itemCode: String): DictionaryItem? = items[itemCode]

    /**
     * 检查字典项编码是否存在
     */
    fun hasItem(itemCode: String): Boolean = items.containsKey(itemCode)

    /**
     * 获取激活的字典项目集合（不可修改的集合）
     */
    fun activeItems(): List<DictionaryItem> =
        items.values.filter { item -> item.status == DictionaryItemStatus.ACTIVE }.toList()

    /**
     * 应用字典创建事件
     */
    @OnSourcing
    fun onCreated(event: DictionaryCreated) {
        this.code = event.code
        this.name = event.name
        this.description = event.description
        this.status = DictionaryStatus.ACTIVE
    }

    /**
     * 应用字典更新事件
     */
    @OnSourcing
    fun onUpdated(event: DictionaryUpdated) {
        this.name = event.name
        this.description = event.description
    }

    /**
     * 应用字典状态改变事件
     */
    @OnSourcing
    fun onStatusChanged(event: DictionaryStatusChanged) {
        this.status = event.status
    }

    /**
     * 应用字典项添加事件
     */
    @OnSourcing
    fun onItemAdded(event: DictionaryItemAdded) {
        val item = DictionaryItem(
            itemCode = event.itemCode,
            itemName = event.itemName,
            itemValue = event.itemValue,
            sortOrder = event.sortOrder,
            description = event.description,
            localizedNames = event.localizedNames,
            status = DictionaryItemStatus.ACTIVE
        )
        items[event.itemCode] = item
    }

    /**
     * 应用字典项更新事件
     */
    @OnSourcing
    fun onItemUpdated(event: DictionaryItemUpdated) {
        items[event.itemCode]?.let { item ->
            items[event.itemCode] = item.copy(
                itemName = event.itemName,
                itemValue = event.itemValue,
                sortOrder = event.sortOrder,
                description = event.description,
                localizedNames = event.localizedNames
            )
        }
    }

    /**
     * 应用字典项状态改变事件
     */
    @OnSourcing
    fun onItemStatusChanged(event: DictionaryItemStatusChanged) {
        items[event.itemCode]?.let { item ->
            items[event.itemCode] = item.copy(status = event.status)
        }
    }

    /**
     * 应用字典项移除事件
     */
    @OnSourcing
    fun onItemRemoved(event: DictionaryItemRemoved) {
        items.remove(event.itemCode)
    }

}