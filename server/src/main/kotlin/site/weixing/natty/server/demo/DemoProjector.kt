package site.weixing.natty.server.demo

import me.ahoo.wow.spring.stereotype.ProjectionProcessorComponent
import org.slf4j.LoggerFactory
import site.weixing.natty.api.demo.demo.DemoCreated

@ProjectionProcessorComponent
class DemoProjector {
    companion object {
        private val log = LoggerFactory.getLogger(DemoProjector::class.java)
    }

    fun onEvent(event: DemoCreated) {
        if (log.isDebugEnabled) {
            log.debug("onEvent: $event")
        }
    }
}
