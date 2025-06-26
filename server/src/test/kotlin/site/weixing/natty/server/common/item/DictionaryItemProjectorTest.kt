// package site.weixing.natty.server.common.item
//
// import com.fasterxml.jackson.databind.ObjectMapper
// import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
// import com.ninjasquad.springmockk.MockkBean
// import io.mockk.Runs
// import io.mockk.every
// import io.mockk.just
// import me.ahoo.wow.eventsourcing.EventStore
// import me.ahoo.wow.eventsourcing.snapshot.SnapshotRepository
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
// import org.springframework.context.annotation.Import
// import site.weixing.natty.api.common.item.DictionaryItemCreated
// import site.weixing.natty.api.common.item.DictionaryItemDeleted
// import site.weixing.natty.api.common.item.DictionaryItemStatusChanged
// import site.weixing.natty.api.common.item.DictionaryItemUpdated
// import site.weixing.natty.domain.common.item.DictionaryItemState
// import site.weixing.natty.domain.common.item.DictionaryItemState.DictionaryItemStatus
// import java.util.Optional
//
// /**
// * 字典项投影器测试
// */
// @DataJpaTest
// @Import(DictionaryItemRepository::class)
// class DictionaryItemProjectorTest(
//    private val dictionaryItemRepository: DictionaryItemRepository
// ) {
//
//    @MockkBean
//    private lateinit var eventStore: EventStore
//
//    @MockkBean
//    private lateinit var snapshotRepository: SnapshotRepository
//
//    private lateinit var dictionaryItemProjector: DictionaryItemProjector
//    private val objectMapper: ObjectMapper = jacksonObjectMapper()
//
//    @BeforeEach
//    fun setup() {
//        dictionaryItemProjector = DictionaryItemProjector(dictionaryItemRepository, objectMapper)
//        every { snapshotRepository.findById<DictionaryItemState>(any(), any()) } returns Optional.empty()
//        every { snapshotRepository.save(any()) } just Runs
//    }
//
//    /**
//     * 测试处理字典项创建事件
//     */
//    @Test
//    fun `should handle DictionaryItemCreated event and save entity`() {
//        val event = DictionaryItemCreated(
//            dictionaryItemId = "test-item-id",
//            dictionaryId = "test-dictionary-id",
//            itemCode = "ITEM_CODE_1",
//            itemName = "测试字典项1",
//            itemValue = "1",
//            sortOrder = 1,
//            description = "这是一个测试字典项",
//            localizedNames = mapOf("en" to "Test Item 1")
//        )
//        val state = DictionaryItemState(
//            id = event.dictionaryItemId,
//            dictionaryId = event.dictionaryId,
//            itemCode = event.itemCode,
//            itemName = event.itemName,
//            itemValue = event.itemValue,
//            sortOrder = event.sortOrder,
//            description = event.description,
//            localizedNames = event.localizedNames,
//            status = DictionaryItemStatus.ACTIVE
//        )
//
//        dictionaryItemProjector.onCreated(event, state)
//
//        val savedEntity = dictionaryItemRepository.findById(event.dictionaryItemId).orElse(null)
//        assert(savedEntity != null)
//        savedEntity?.apply {
//            assert(id == event.dictionaryItemId)
//            assert(dictionaryId == event.dictionaryId)
//            assert(itemCode == event.itemCode)
//            assert(itemName == event.itemName)
//            assert(itemValue == event.itemValue)
//            assert(sortOrder == event.sortOrder)
//            assert(description == event.description)
//            assert(status == state.status)
//            assert(localizedNames == objectMapper.writeValueAsString(event.localizedNames))
//        }
//    }
//
//    /**
//     * 测试处理字典项更新事件
//     */
//    @Test
//    fun `should handle DictionaryItemUpdated event and update entity`() {
//        val itemId = "test-item-id"
//        val initialEntity = DictionaryItemEntity(
//            id = itemId,
//            dictionaryId = "test-dictionary-id",
//            itemCode = "OLD_ITEM",
//            itemName = "旧字典项",
//            itemValue = "old",
//            sortOrder = 0,
//            description = "旧描述",
//            status = DictionaryItemStatus.ACTIVE,
//            localizedNames = "{}"
//        )
//        dictionaryItemRepository.save(initialEntity)
//
//        val event = DictionaryItemUpdated(
//            dictionaryItemId = itemId,
//            itemName = "新字典项",
//            itemValue = "new",
//            sortOrder = 1,
//            description = "新描述",
//            localizedNames = mapOf("en" to "New Item")
//        )
//        val state = DictionaryItemState(
//            id = itemId,
//            dictionaryId = initialEntity.dictionaryId,
//            itemCode = initialEntity.itemCode,
//            itemName = event.itemName,
//            itemValue = event.itemValue,
//            sortOrder = event.sortOrder,
//            description = event.description,
//            localizedNames = event.localizedNames,
//            status = DictionaryItemStatus.ACTIVE
//        )
//
//        dictionaryItemProjector.onUpdated(event, state)
//
//        val updatedEntity = dictionaryItemRepository.findById(itemId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(itemName == event.itemName)
//            assert(itemValue == event.itemValue)
//            assert(sortOrder == event.sortOrder)
//            assert(description == event.description)
//            assert(localizedNames == objectMapper.writeValueAsString(event.localizedNames))
//            assert(status == state.status)
//        }
//    }
//
//    /**
//     * 测试处理字典项状态改变事件
//     */
//    @Test
//    fun `should handle DictionaryItemStatusChanged event and update entity status`() {
//        val itemId = "test-item-id"
//        val initialEntity = DictionaryItemEntity(
//            id = itemId,
//            dictionaryId = "test-dictionary-id",
//            itemCode = "TEST_ITEM",
//            itemName = "测试项",
//            itemValue = "test",
//            sortOrder = 0,
//            description = "描述",
//            status = DictionaryItemStatus.ACTIVE,
//            localizedNames = "{}"
//        )
//        dictionaryItemRepository.save(initialEntity)
//
//        val event = DictionaryItemStatusChanged(
//            dictionaryItemId = itemId,
//            status = DictionaryItemStatus.INACTIVE.name
//        )
//        val state = DictionaryItemState(
//            id = itemId,
//            dictionaryId = initialEntity.dictionaryId,
//            itemCode = initialEntity.itemCode,
//            itemName = initialEntity.itemName,
//            itemValue = initialEntity.itemValue,
//            sortOrder = initialEntity.sortOrder,
//            description = initialEntity.description,
//            localizedNames = initialEntity.localizedNames?.let { objectMapper.readValue(it, Map::class.java) as Map<String, String> },
//            status = DictionaryItemStatus.INACTIVE
//        )
//
//        dictionaryItemProjector.onStatusChanged(event, state)
//
//        val updatedEntity = dictionaryItemRepository.findById(itemId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(status == DictionaryItemStatus.INACTIVE)
//        }
//    }
//
//    /**
//     * 测试处理字典项删除事件
//     */
//    @Test
//    fun `should handle DictionaryItemDeleted event and update entity status to DELETED`() {
//        val itemId = "test-item-id"
//        val initialEntity = DictionaryItemEntity(
//            id = itemId,
//            dictionaryId = "test-dictionary-id",
//            itemCode = "TEST_ITEM",
//            itemName = "测试项",
//            itemValue = "test",
//            sortOrder = 0,
//            description = "描述",
//            status = DictionaryItemStatus.ACTIVE,
//            localizedNames = "{}"
//        )
//        dictionaryItemRepository.save(initialEntity)
//
//        val event = DictionaryItemDeleted(
//            dictionaryItemId = itemId
//        )
//        val state = DictionaryItemState(
//            id = itemId,
//            dictionaryId = initialEntity.dictionaryId,
//            itemCode = initialEntity.itemCode,
//            itemName = initialEntity.itemName,
//            itemValue = initialEntity.itemValue,
//            sortOrder = initialEntity.sortOrder,
//            description = initialEntity.description,
//            localizedNames = initialEntity.localizedNames?.let { objectMapper.readValue(it, Map::class.java) as Map<String, String> },
//            status = DictionaryItemStatus.DELETED
//        )
//
//        dictionaryItemProjector.onDeleted(event, state)
//
//        val updatedEntity = dictionaryItemRepository.findById(itemId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(status == DictionaryItemStatus.DELETED)
//        }
//    }
// }
