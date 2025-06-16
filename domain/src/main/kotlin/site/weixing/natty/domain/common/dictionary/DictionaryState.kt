package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.common.dictionary.DictionaryCreated
import site.weixing.natty.api.common.dictionary.DictionaryStatusChanged
import site.weixing.natty.api.common.dictionary.DictionaryUpdated

/**
 * 字典状态
 *
 * @param id 字典ID
 * @param code 字典编码
 * @param name 字典名称
 * @param description 字典描述
 * @param status 字典状态
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

    /**
     * 应用字典创建事件
     *
     * @param event 字典创建事件
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
     *
     * @param event 字典更新事件
     */
    @OnSourcing
    fun onUpdated(event: DictionaryUpdated) {
        this.name = event.name
        this.description = event.description
    }

    /**
     * 应用字典状态改变事件
     *
     * @param event 字典状态改变事件
     */
    @OnSourcing
    fun onStatusChanged(event: DictionaryStatusChanged) {
        this.status = DictionaryStatus.valueOf(event.status)
    }

    /**
     * 字典状态枚举
     */
    enum class DictionaryStatus {
        /** 启用 */
        ACTIVE,

        /** 禁用 */
        INACTIVE,

        /** 删除 */
        DELETED
    }
} 