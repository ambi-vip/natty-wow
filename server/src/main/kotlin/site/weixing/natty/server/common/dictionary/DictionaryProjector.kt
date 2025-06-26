// package site.weixing.natty.server.dictionary.dictionary
//
// import me.ahoo.wow.api.annotation.OnEvent
// import me.ahoo.wow.api.annotation.WaitForSnapshot
// import org.slf4j.LoggerFactory
// import org.springframework.stereotype.Component
// import site.weixing.natty.api.dictionary.dictionary.DictionaryCreated
// import site.weixing.natty.api.dictionary.dictionary.DictionaryDeleted
// import site.weixing.natty.api.dictionary.dictionary.DictionaryStatusChanged
// import site.weixing.natty.api.dictionary.dictionary.DictionaryUpdated
// import site.weixing.natty.domain.dictionary.dictionary.DictionaryState
//
// /**
// * 字典投影器
// *
// * 订阅字典相关的事件，并将数据同步到`dictionary_read_model`表。
// */
// @Component
// class DictionaryProjector(
//    private val dictionaryRepository: DictionaryRepository
// ) {
//
//    companion object {
//        private val log = LoggerFactory.getLogger(DictionaryProjector::class.java)
//    }
//
//    /**
//     * 处理字典创建事件
//     *
//     * @param event 字典创建事件
//     * @param state 字典状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onCreated(event: DictionaryCreated, state: DictionaryState) {
//        if (log.isDebugEnabled) {
//            log.debug("onCreated: {}.", event)
//        }
//        val entity = DictionaryEntity(
//            id = event.dictionaryId,
//            code = event.code,
//            name = event.name,
//            description = event.description,
//            status = state.status
//        )
//        dictionaryRepository.save(entity)
//    }
//
//    /**
//     * 处理字典更新事件
//     *
//     * @param event 字典更新事件
//     * @param state 字典状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onUpdated(event: DictionaryUpdated, state: DictionaryState) {
//        if (log.isDebugEnabled) {
//            log.debug("onUpdated: {}.", event)
//        }
//        val entity = dictionaryRepository.findById(event.dictionaryId).orElse(null)
//            ?: throw IllegalStateException("字典实体[${event.dictionaryId}]不存在，无法更新。")
//
//        entity.name = event.name
//        entity.description = event.description
//        entity.status = state.status
//
//        dictionaryRepository.save(entity)
//    }
//
//    /**
//     * 处理字典状态改变事件
//     *
//     * @param event 字典状态改变事件
//     * @param state 字典状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onStatusChanged(event: DictionaryStatusChanged, state: DictionaryState) {
//        if (log.isDebugEnabled) {
//            log.debug("onStatusChanged: {}.", event)
//        }
//        val entity = dictionaryRepository.findById(event.dictionaryId).orElse(null)
//            ?: throw IllegalStateException("字典实体[${event.dictionaryId}]不存在，无法改变状态。")
//
//        entity.status = state.status
//        dictionaryRepository.save(entity)
//    }
//
//    /**
//     * 处理字典删除事件
//     *
//     * @param event 字典删除事件
//     * @param state 字典状态快照
//     */
//    @OnEvent
//    @WaitForSnapshot
//    fun onDeleted(event: DictionaryDeleted, state: DictionaryState) {
//        if (log.isDebugEnabled) {
//            log.debug("onDeleted: {}.", event)
//        }
//        val entity = dictionaryRepository.findById(event.dictionaryId).orElse(null)
//            ?: throw IllegalStateException("字典实体[${event.dictionaryId}]不存在，无法删除。")
//
//        entity.status = state.status // 逻辑删除，状态置为DELETED
//        dictionaryRepository.save(entity)
//    }
// }
