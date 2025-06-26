// package site.weixing.natty.server.common.dictionary
//
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
// import org.springframework.test.context.ContextConfiguration
// import site.weixing.natty.api.common.dictionary.DictionaryCreated
// import site.weixing.natty.api.common.dictionary.DictionaryDeleted
// import site.weixing.natty.api.common.dictionary.DictionaryStatusChanged
// import site.weixing.natty.api.common.dictionary.DictionaryUpdated
// import site.weixing.natty.domain.common.dictionary.DictionaryState
// import site.weixing.natty.domain.common.dictionary.DictionaryState.DictionaryStatus
// import java.util.Optional
//
// /**
// * 字典投影器测试
// */
// @DataJpaTest
// @ContextConfiguration(
//    classes = [
//        DictionaryProjector::class,
//        // Spring Data JPA 自动配置
//        DictionaryRepository::class
//    ]
// )
// @Import(DictionaryRepository::class) // 导入Repository，避免Spring Test自动扫描问题
// class DictionaryProjectorTest(
//    private val dictionaryRepository: DictionaryRepository
// ) {
//
//    @MockkBean
//    private lateinit var eventStore: EventStore
//
//    @MockkBean
//    private lateinit var snapshotRepository: SnapshotRepository
//
//    private lateinit var dictionaryProjector: DictionaryProjector
//
//    @BeforeEach
//    fun setup() {
//        dictionaryProjector = DictionaryProjector(dictionaryRepository)
//        every { snapshotRepository.findById<DictionaryState>(any(), any()) } returns Optional.empty()
//        every { snapshotRepository.save(any()) } just Runs
//    }
//
//    /**
//     * 测试处理字典创建事件
//     */
//    @Test
//    fun `should handle DictionaryCreated event and save entity`() {
//        val event = DictionaryCreated(
//            dictionaryId = "test-dictionary-id",
//            code = "TEST_CODE",
//            name = "测试字典",
//            description = "这是一个测试字典"
//        )
//        val state = DictionaryState(
//            id = event.dictionaryId,
//            code = event.code,
//            name = event.name,
//            description = event.description,
//            status = DictionaryStatus.ACTIVE
//        )
//
//        dictionaryProjector.onCreated(event, state)
//
//        val savedEntity = dictionaryRepository.findById(event.dictionaryId).orElse(null)
//        assert(savedEntity != null)
//        savedEntity?.apply {
//            assert(id == event.dictionaryId)
//            assert(code == event.code)
//            assert(name == event.name)
//            assert(description == event.description)
//            assert(status == state.status)
//        }
//    }
//
//    /**
//     * 测试处理字典更新事件
//     */
//    @Test
//    fun `should handle DictionaryUpdated event and update entity`() {
//        val dictionaryId = "test-dictionary-id"
//        val initialEntity = DictionaryEntity(
//            id = dictionaryId,
//            code = "OLD_CODE",
//            name = "旧字典",
//            description = "旧描述",
//            status = DictionaryStatus.ACTIVE
//        )
//        dictionaryRepository.save(initialEntity)
//
//        val event = DictionaryUpdated(
//            dictionaryId = dictionaryId,
//            name = "新字典",
//            description = "新描述"
//        )
//        val state = DictionaryState(
//            id = dictionaryId,
//            code = initialEntity.code,
//            name = event.name,
//            description = event.description,
//            status = DictionaryStatus.ACTIVE
//        )
//
//        dictionaryProjector.onUpdated(event, state)
//
//        val updatedEntity = dictionaryRepository.findById(dictionaryId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(name == event.name)
//            assert(description == event.description)
//            assert(status == state.status)
//        }
//    }
//
//    /**
//     * 测试处理字典状态改变事件
//     */
//    @Test
//    fun `should handle DictionaryStatusChanged event and update entity status`() {
//        val dictionaryId = "test-dictionary-id"
//        val initialEntity = DictionaryEntity(
//            id = dictionaryId,
//            code = "TEST_CODE",
//            name = "测试字典",
//            description = "描述",
//            status = DictionaryStatus.ACTIVE
//        )
//        dictionaryRepository.save(initialEntity)
//
//        val event = DictionaryStatusChanged(
//            dictionaryId = dictionaryId,
//            status = DictionaryStatus.INACTIVE.name
//        )
//        val state = DictionaryState(
//            id = dictionaryId,
//            code = initialEntity.code,
//            name = initialEntity.name,
//            description = initialEntity.description,
//            status = DictionaryStatus.INACTIVE
//        )
//
//        dictionaryProjector.onStatusChanged(event, state)
//
//        val updatedEntity = dictionaryRepository.findById(dictionaryId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(status == DictionaryStatus.INACTIVE)
//        }
//    }
//
//    /**
//     * 测试处理字典删除事件
//     */
//    @Test
//    fun `should handle DictionaryDeleted event and update entity status to DELETED`() {
//        val dictionaryId = "test-dictionary-id"
//        val initialEntity = DictionaryEntity(
//            id = dictionaryId,
//            code = "TEST_CODE",
//            name = "测试字典",
//            description = "描述",
//            status = DictionaryStatus.ACTIVE
//        )
//        dictionaryRepository.save(initialEntity)
//
//        val event = DictionaryDeleted(
//            dictionaryId = dictionaryId
//        )
//        val state = DictionaryState(
//            id = dictionaryId,
//            code = initialEntity.code,
//            name = initialEntity.name,
//            description = initialEntity.description,
//            status = DictionaryStatus.DELETED
//        )
//
//        dictionaryProjector.onDeleted(event, state)
//
//        val updatedEntity = dictionaryRepository.findById(dictionaryId).orElse(null)
//        assert(updatedEntity != null)
//        updatedEntity?.apply {
//            assert(status == DictionaryStatus.DELETED)
//        }
//    }
// }
