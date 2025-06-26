package site.weixing.natty.domain.demo

import me.ahoo.wow.test.SagaVerifier.sagaVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import site.weixing.natty.api.demo.demo.DemoCreated
import site.weixing.natty.api.demo.demo.UpdateDemo

class DemoSagaTest {

    @Test
    fun onCreated() {
        val event = DemoCreated("data")
        sagaVerifier<DemoSaga>()
            .`when`(
                event
            )
            .expectCommandBody<UpdateDemo> {
                assertThat(it.data).isEqualTo("updated")
            }
            .verify()
    }
}
