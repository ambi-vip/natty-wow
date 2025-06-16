package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
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
 * 字典聚合根测试
 */
class DictionaryTest {

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
                assertThat(it.code, equalTo(command.code))
                assertThat(it.name, equalTo(command.name))
                assertThat(it.description, equalTo(command.description))
                assertThat(it.status, equalTo(DictionaryStatus.ACTIVE))
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
                assertThat(it.name, equalTo(command.name))
                assertThat(it.description, equalTo(command.description))
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
            status = DictionaryStatus.INACTIVE.name
        )

        aggregateVerifier<Dictionary, DictionaryState>()
            .given(givenEvent)
            .`when`(command)
            .expectNoError()
            .expectEventType(DictionaryStatusChanged::class.java)
            .expectState {
                assertThat(it.status, equalTo(DictionaryStatus.INACTIVE))
            }
            .verify()
    }


} 