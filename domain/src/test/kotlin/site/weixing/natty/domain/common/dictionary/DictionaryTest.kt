package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.weixing.natty.domain.TestPrepareKeyFactory
import site.weixing.natty.api.common.dictionary.AddDictionaryItem
import site.weixing.natty.api.common.dictionary.ChangeDictionaryItemStatus
import site.weixing.natty.api.common.dictionary.ChangeDictionaryStatus
import site.weixing.natty.api.common.dictionary.CreateDictionary
import site.weixing.natty.api.common.dictionary.DictionaryCreated
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
 * 字典聚合根测试
 */
class DictionaryTest {

    /**
     * 初始化测试环境
     */
    @BeforeEach
    fun setUp() {
        // 初始化PrepareKey工厂
        val prepareKeyFactory = TestPrepareKeyFactory.create()
        val dictionaryPrepares = DictionaryPrepares(prepareKeyFactory)
    }

    /**
     * 测试创建字典
     */
    @Test
    fun `should create dictionary when CreateDictionary command is received`() {
        val command = CreateDictionary(
            code = "TEST_CODE",
            name = "测试字典",
            description = "这是一个测试字典"
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryCreated::class.java)
            .expectState {
                assertThat(it.code).isEqualTo(command.code)
                assertThat(it.name).isEqualTo(command.name)
                assertThat(it.description).isEqualTo(command.description)
                assertThat(it.status).isEqualTo(DictionaryStatus.ACTIVE)
            }
            .verify()
    }

    /**
     * 测试更新字典
     */
    @Test
    fun `should update dictionary when UpdateDictionary command is received`() {
        val dictionaryId = "test-dictionary-id"
        val givenEvent = DictionaryCreated(
            dictionaryId = dictionaryId,
            code = "OLD_CODE",
            name = "旧字典",
            description = "旧描述"
        )
        val command = UpdateDictionary(
            id = dictionaryId,
            name = "新字典",
            description = "新描述"
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryUpdated::class.java)
            .expectState {
                assertThat(it.name).isEqualTo(command.name)
                assertThat(it.description).isEqualTo(command.description)
            }
            .verify()
    }

    /**
     * 测试改变字典状态
     */
    @Test
    fun `should change dictionary status when ChangeDictionaryStatus command is received`() {
        val dictionaryId = "test-dictionary-id"
        val givenEvent = DictionaryCreated(
            dictionaryId = dictionaryId,
            code = "TEST_CODE",
            name = "测试字典",
            description = "这是一个测试字典"
        )
        val command = ChangeDictionaryStatus(
            id = dictionaryId,
            status = DictionaryStatus.INACTIVE
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryStatusChanged::class.java)
            .expectState {
                assertThat(it.status).isEqualTo(DictionaryStatus.INACTIVE)
            }
            .verify()
    }

    /**
     * 测试添加字典项
     */
    @Test
    fun `should add dictionary item when AddDictionaryItem command is received`() {
        val dictionaryId = "test-dictionary-id"
        val givenEvent = DictionaryCreated(
            dictionaryId = dictionaryId,
            code = "TEST_CODE",
            name = "测试字典",
            description = "这是一个测试字典"
        )
        val command = AddDictionaryItem(
            itemCode = "ITEM001",
            itemName = "测试项目",
            itemValue = "测试值",
            sortOrder = 1,
            description = "测试字典项",
            localizedNames = mapOf("en" to "Test Item")
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemAdded::class.java)
            .expectState {
                assertThat(it.hasItem(command.itemCode)).isTrue
                val item = it.getItem(command.itemCode)
                assertThat(item?.itemName).isEqualTo(command.itemName)
                assertThat(item?.itemValue).isEqualTo(command.itemValue)
                assertThat(item?.status).isEqualTo(DictionaryItemStatus.ACTIVE)
            }
            .verify()
    }

    /**
     * 测试更新字典项
     */
    @Test
    fun `should update dictionary item when UpdateDictionaryItem command is received`() {
        val dictionaryId = "test-dictionary-id"
        val itemCode = "ITEM001"
        val givenEvents = listOf(
            DictionaryCreated(
                dictionaryId = dictionaryId,
                code = "TEST_CODE",
                name = "测试字典",
                description = "这是一个测试字典"
            ),
            DictionaryItemAdded(
                dictionaryId = dictionaryId,
                itemCode = itemCode,
                itemName = "旧项目名",
                itemValue = "旧值",
                sortOrder = 1,
                description = "旧描述"
            )
        )
        val command = UpdateDictionaryItem(
            itemCode = itemCode,
            itemName = "新项目名",
            itemValue = "新值",
            sortOrder = 2,
            description = "新描述"
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(*givenEvents.toTypedArray())
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemUpdated::class.java)
            .expectState {
                val item = it.getItem(itemCode)
                assertThat(item?.itemName).isEqualTo(command.itemName)
                assertThat(item?.itemValue).isEqualTo(command.itemValue)
                assertThat(item?.sortOrder).isEqualTo(command.sortOrder)
                assertThat(item?.description).isEqualTo(command.description)
            }
            .verify()
    }

    /**
     * 测试改变字典项状态
     */
    @Test
    fun `should change dictionary item status when ChangeDictionaryItemStatus command is received`() {
        val dictionaryId = "test-dictionary-id"
        val itemCode = "ITEM001"
        val givenEvents = listOf(
            DictionaryCreated(
                dictionaryId = dictionaryId,
                code = "TEST_CODE",
                name = "测试字典",
                description = "这是一个测试字典"
            ),
            DictionaryItemAdded(
                dictionaryId = dictionaryId,
                itemCode = itemCode,
                itemName = "测试项目",
                itemValue = "测试值",
                sortOrder = 1
            )
        )
        val command = ChangeDictionaryItemStatus(
            itemCode = itemCode,
            status = DictionaryItemStatus.INACTIVE
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(*givenEvents.toTypedArray())
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemStatusChanged::class.java)
            .expectState {
                val item = it.getItem(itemCode)
                assertThat(item?.status).isEqualTo(DictionaryItemStatus.INACTIVE)
            }
            .verify()
    }

    /**
     * 测试移除字典项
     */
    @Test
    fun `should remove dictionary item when RemoveDictionaryItem command is received`() {
        val dictionaryId = "test-dictionary-id"
        val itemCode = "ITEM001"
        val givenEvents = listOf(
            DictionaryCreated(
                dictionaryId = dictionaryId,
                code = "TEST_CODE",
                name = "测试字典",
                description = "这是一个测试字典"
            ),
            DictionaryItemAdded(
                dictionaryId = dictionaryId,
                itemCode = itemCode,
                itemName = "测试项目",
                itemValue = "测试值",
                sortOrder = 1
            )
        )
        val command = RemoveDictionaryItem(
            itemCode = itemCode
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(*givenEvents.toTypedArray())
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemRemoved::class.java)
            .expectState {
                assertThat(it.hasItem(itemCode)).isFalse
            }
            .verify()
    }

    /**
     * 测试重复添加字典项时应该失败
     */
    @Test
    fun `should fail when adding duplicate dictionary item code`() {
        val dictionaryId = "test-dictionary-id"
        val itemCode = "ITEM001"
        val givenEvents = listOf(
            DictionaryCreated(
                dictionaryId = dictionaryId,
                code = "TEST_CODE",
                name = "测试字典",
                description = "这是一个测试字典"
            ),
            DictionaryItemAdded(
                dictionaryId = dictionaryId,
                itemCode = itemCode,
                itemName = "已存在项目",
                itemValue = "已存在值",
                sortOrder = 1
            )
        )
        val command = AddDictionaryItem(
            itemCode = itemCode, // 重复的编码
            itemName = "新项目",
            itemValue = "新值",
            sortOrder = 2
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(*givenEvents.toTypedArray())
            .`when`(command)
            .expectError()
            .verify()
    }

    /**
     * 测试在非活跃状态字典上添加字典项应该失败
     */
    @Test
    fun `should fail when adding item to inactive dictionary`() {
        val dictionaryId = "test-dictionary-id"
        val givenEvents = listOf(
            DictionaryCreated(
                dictionaryId = dictionaryId,
                code = "TEST_CODE",
                name = "测试字典",
                description = "这是一个测试字典"
            ),
            DictionaryStatusChanged(
                dictionaryId = dictionaryId,
                status = DictionaryStatus.INACTIVE
            )
        )
        val command = AddDictionaryItem(
            itemCode = "ITEM001",
            itemName = "测试项目",
            itemValue = "测试值"
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(*givenEvents.toTypedArray())
            .`when`(command)
            .expectError()
            .verify()
    }
}
