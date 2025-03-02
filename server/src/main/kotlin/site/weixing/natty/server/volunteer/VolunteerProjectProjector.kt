package site.weixing.natty.server.volunteer

import me.ahoo.wow.spring.stereotype.ProjectionProcessor
import org.slf4j.LoggerFactory
import site.weixing.natty.api.volunteer.VolunteerProjectCreated

@ProjectionProcessor
class VolunteerProjectProjector {
    companion object {
        private val log = LoggerFactory.getLogger(VolunteerProjectProjector::class.java)
    }

    fun onEvent(event: VolunteerProjectCreated) {
        if (log.isDebugEnabled) {
            log.debug("onEvent: $event")
        }
    }
}
