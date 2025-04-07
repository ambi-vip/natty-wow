package site.weixing.natty.domain.demo

import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.api.modeling.AggregateId
import me.ahoo.wow.spring.stereotype.StatelessSaga
import site.weixing.natty.demo.api.demo.DemoCreated
import site.weixing.natty.demo.api.demo.UpdateDemo

@StatelessSaga
class DemoSaga {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(DemoSaga::class.java)
    }

    @OnEvent
    fun onCreated(event: DemoCreated, aggregateId: AggregateId): UpdateDemo {
        if (log.isDebugEnabled) {
            log.debug("onCreated: $event")
        }
        return UpdateDemo(
            id = aggregateId.id,
            data = "updated"
        )
    }
}
