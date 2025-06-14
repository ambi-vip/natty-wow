package site.weixing.natty.domain.common.item

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import site.weixing.natty.api.common.item.ChangeDictionaryItemStatus
import site.weixing.natty.api.common.item.CreateDictionaryItem
import site.weixing.natty.api.common.item.DeleteDictionaryItem
import site.weixing.natty.api.common.item.DictionaryItemCreated
import site.weixing.natty.api.common.item.DictionaryItemDeleted
import site.weixing.natty.api.common.item.DictionaryItemStatusChanged
import site.weixing.natty.api.common.item.DictionaryItemUpdated
import site.weixing.natty.api.common.item.UpdateDictionaryItem
import site.weixing.natty.domain.common.item.DictionaryItemState.DictionaryItemStatus

/**
 * 字典项聚合根测试
 */
class DictionaryItemTest {

    /**
     * 测试创建字典项
     */
    @Test
    fun `should create dictionary item when CreateDictionaryItem command is received`() {
        val command = CreateDictionaryItem(
            dictionaryId = "test-dictionary-id",
            itemCode = "ITEM_CODE_1",
            itemName = "测试字典项1",
            itemValue = "1",
            sortOrder = 1,
            description = "这是一个测试字典项",
            localizedNames = mapOf("en" to "Test Item 1")
        )

        aggregateVerifier<DictionaryItem, DictionaryItemState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemCreated::class.java)
            .expectState {
                assertThat(it.dictionaryId, equalTo(command.dictionaryId))
                assertThat(it.itemCode, equalTo(command.itemCode))
                assertThat(it.itemName, equalTo(command.itemName))
                assertThat(it.itemValue, equalTo(command.itemValue))
                assertThat(it.sortOrder, equalTo(command.sortOrder))
                assertThat(it.description, equalTo(command.description))
                assertThat(it.localizedNames, equalTo(command.localizedNames))
                assertThat(it.status, equalTo(DictionaryItemStatus.ACTIVE))
            }
            .verify()
    }

    /**
     * 测试更新字典项
     */
    @Test
    fun `should update dictionary item when UpdateDictionaryItem command is received`() {
        val dictionaryItemId = "test-dictionary-item-id"
        val givenEvent = DictionaryItemCreated(
            dictionaryItemId = dictionaryItemId,
            dictionaryId = "test-dictionary-id",
            itemCode = "OLD_ITEM_CODE",
            itemName = "旧字典项",
            itemValue = "old",
            sortOrder = 0,
            description = "旧描述",
            localizedNames = mapOf("en" to "Old Item")
        )
        val command = UpdateDictionaryItem(
            id = dictionaryItemId,
            itemName = "新字典项",
            itemValue = "new",
            sortOrder = 1,
            description = "新描述",
            localizedNames = mapOf("en" to "New Item")
        )

        aggregateVerifier<DictionaryItem, DictionaryItemState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemUpdated::class.java)
            .expectState {
                assertThat(it.itemName, equalTo(command.itemName))
                assertThat(it.itemValue, equalTo(command.itemValue))
                assertThat(it.sortOrder, equalTo(command.sortOrder))
                assertThat(it.description, equalTo(command.description))
                assertThat(it.localizedNames, equalTo(command.localizedNames))
            }
            .verify()
    }

    /**
     * 测试改变字典项状态
     */
    @Test
    fun `should change dictionary item status when ChangeDictionaryItemStatus command is received`() {
        val dictionaryItemId = "test-dictionary-item-id"
        val givenEvent = DictionaryItemCreated(
            dictionaryItemId = dictionaryItemId,
            dictionaryId = "test-dictionary-id",
            itemCode = "TEST_ITEM_CODE",
            itemName = "测试字典项",
            itemValue = "test",
            sortOrder = 0,
            description = "描述"
        )
        val command = ChangeDictionaryItemStatus(
            id = dictionaryItemId,
            status = DictionaryItemStatus.INACTIVE.name
        )

        aggregateVerifier<DictionaryItem, DictionaryItemState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemStatusChanged::class.java)
            .expectState {
                assertThat(it.status, equalTo(DictionaryItemStatus.INACTIVE))
            }
            .verify()
    }

    /**
     * 测试删除字典项
     */
    @Test
    fun `should delete dictionary item when DeleteDictionaryItem command is received`() {
        val dictionaryItemId = "test-dictionary-item-id"
        val givenEvent = DictionaryItemCreated(
            dictionaryItemId = dictionaryItemId,
            dictionaryId = "test-dictionary-id",
            itemCode = "TEST_ITEM_CODE",
            itemName = "测试字典项",
            itemValue = "test",
            sortOrder = 0,
            description = "描述"
        )
        val command = DeleteDictionaryItem(
            id = dictionaryItemId
        )

        aggregateVerifier<DictionaryItem, DictionaryItemState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryItemDeleted::class.java)
            .expectState {
                assertThat(it.status, equalTo(DictionaryItemStatus.DELETED))
            }
            .verify()
    }
} 