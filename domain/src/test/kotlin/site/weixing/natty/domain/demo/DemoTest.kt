package site.weixing.natty.domain.demo

import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import site.weixing.natty.api.demo.demo.CreateDemo
import site.weixing.natty.api.demo.demo.DemoCreated
import site.weixing.natty.api.demo.demo.DemoUpdated
import site.weixing.natty.api.demo.demo.UpdateDemo

class DemoTest {

    @Test
    fun onCreate() {
        val command = CreateDemo(
            data = "data"
        )

        aggregateVerifier<Demo, DemoState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(DemoCreated::class.java)
            .expectState {
                assertThat(it.data).isEqualTo(command.data)
            }
            .verify()
    }

    @Test
    fun onUpdate() {
        val command = UpdateDemo(
            id = GlobalIdGenerator.generateAsString(),
            data = "data"
        )

        aggregateVerifier<Demo, DemoState>()
            .given(DemoCreated("old"))
            .`when`(command)
            .expectNoError()
            .expectEventType(DemoUpdated::class.java)
            .expectState {
                assertThat(it.data).isEqualTo(command.data)
            }
            .verify()
    }
}
