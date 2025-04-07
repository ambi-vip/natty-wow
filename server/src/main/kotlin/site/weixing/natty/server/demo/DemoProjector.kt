package site.weixing.natty.server.demo

import me.ahoo.wow.spring.stereotype.ProjectionProcessor
import site.weixing.natty.demo.api.demo.DemoCreated
import org.slf4j.LoggerFactory

@ProjectionProcessor
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
