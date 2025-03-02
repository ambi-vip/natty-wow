package site.weixing.natty.domain.demo

import me.ahoo.wow.test.SagaVerifier.sagaVerifier
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import site.weixing.natty.api.demo.DemoCreated
import site.weixing.natty.api.demo.UpdateDemo

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
