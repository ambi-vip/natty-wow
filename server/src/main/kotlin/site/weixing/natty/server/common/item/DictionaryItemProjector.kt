// package site.weixing.natty.server.common.item
//
// import com.fasterxml.jackson.databind.ObjectMapper
// import me.ahoo.wow.api.annotation.OnEvent
// import me.ahoo.wow.api.annotation.WaitForSnapshot
// import org.slf4j.LoggerFactory
// import org.springframework.stereotype.Component
// import site.weixing.natty.api.common.item.DictionaryItemCreated
// import site.weixing.natty.api.common.item.DictionaryItemDeleted
// import site.weixing.natty.api.common.item.DictionaryItemStatusChanged
// import site.weixing.natty.api.common.item.DictionaryItemUpdated
// import site.weixing.natty.domain.common.item.DictionaryItemState
//
// /**
// * 字典项投影器
// *
// * 订阅字典项相关的事件，并将数据同步到`dictionary_item_read_model`表。
// *
// * @param dictionaryItemRepository 字典项仓库
// * @param objectMapper JSON对象映射器
// */
// @Component
// class DictionaryItemProjector(
//    private val dictionaryItemRepository: DictionaryItemRepository,
//    private val objectMapper: ObjectMapper
// ) {
//
//    companion object {
//        private val log = LoggerFactory.getLogger(DictionaryItemProjector::class.java)
//    }
//
//    /**
//     * 处理字典项创建事件
//     *
//     * @param event 字典项创建事件
//     * @param state 字典项状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onCreated(event: DictionaryItemCreated, state: DictionaryItemState) {
//        if (log.isDebugEnabled) {
//            log.debug("onCreated: {}.", event)
//        }
//        val entity = DictionaryItemEntity(
//            id = event.dictionaryItemId,
//            dictionaryId = event.dictionaryId,
//            itemCode = event.itemCode,
//            itemName = event.itemName,
//            itemValue = event.itemValue,
//            sortOrder = event.sortOrder,
//            description = event.description,
//            status = state.status,
//            localizedNames = event.localizedNames?.let { objectMapper.writeValueAsString(it) }
//        )
//        dictionaryItemRepository.save(entity)
//    }
//
//    /**
//     * 处理字典项更新事件
//     *
//     * @param event 字典项更新事件
//     * @param state 字典项状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onUpdated(event: DictionaryItemUpdated, state: DictionaryItemState) {
//        if (log.isDebugEnabled) {
//            log.debug("onUpdated: {}.", event)
//        }
//        val entity = dictionaryItemRepository.findById(event.dictionaryItemId).orElse(null)
//            ?: throw IllegalStateException("字典项实体[${event.dictionaryItemId}]不存在，无法更新。")
//
//        entity.itemName = event.itemName
//        entity.itemValue = event.itemValue
//        entity.sortOrder = event.sortOrder
//        entity.description = event.description
//        entity.localizedNames = event.localizedNames?.let { objectMapper.writeValueAsString(it) }
//        entity.status = state.status
//
//        dictionaryItemRepository.save(entity)
//    }
//
//    /**
//     * 处理字典项状态改变事件
//     *
//     * @param event 字典项状态改变事件
//     * @param state 字典项状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onStatusChanged(event: DictionaryItemStatusChanged, state: DictionaryItemState) {
//        if (log.isDebugEnabled) {
//            log.debug("onStatusChanged: {}.", event)
//        }
//        val entity = dictionaryItemRepository.findById(event.dictionaryItemId).orElse(null)
//            ?: throw IllegalStateException("字典项实体[${event.dictionaryItemId}]不存在，无法改变状态。")
//
//        entity.status = state.status
//        dictionaryItemRepository.save(entity)
//    }
//
//    /**
//     * 处理字典项删除事件
//     *
//     * @param event 字典项删除事件
//     * @param state 字典项状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onDeleted(event: DictionaryItemDeleted, state: DictionaryItemState) {
//        if (log.isDebugEnabled) {
//            log.debug("onDeleted: {}.", event)
//        }
//        val entity = dictionaryItemRepository.findById(event.dictionaryItemId).orElse(null)
//            ?: throw IllegalStateException("字典项实体[${event.dictionaryItemId}]不存在，无法删除。")
//
//        entity.status = state.status // 逻辑删除，状态置为DELETED
//        dictionaryItemRepository.save(entity)
//    }
// }
