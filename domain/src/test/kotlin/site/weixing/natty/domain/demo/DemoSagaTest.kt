package site.weixing.natty.domain.demo

import site.weixing.natty.demo.api.demo.DemoCreated
import site.weixing.natty.demo.api.demo.UpdateDemo
import me.ahoo.wow.test.SagaVerifier.sagaVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class DemoSagaTest {

    @Test
    fun onCreated() {
        val event = DemoCreated("data")
        sagaVerifier<DemoSaga>()
            .`when`(
                event
            )
            .expectCommandBody<UpdateDemo> {
                assertThat(it.data, equalTo("updated"))
            }
            .verify()
    }
}
