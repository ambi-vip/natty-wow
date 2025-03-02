package site.weixing.natty.domain.volunteer

import io.swagger.v3.oas.annotations.media.Schema
import me.ahoo.wow.api.Identifier
import site.weixing.natty.api.volunteer.VolunteerProjectCreated

/**
 * VolunteerProjectState
 * @author ambi
 */
@Schema(name = "VolunteerProjectState")
class VolunteerProjectState(override val id: String) : Identifier {

    lateinit var name: String
        private set
    lateinit var code: String
        private set
    var status = VolunteerProjectStatus.CREATED
        private set

    fun onSourcing(volunteerProjectCreated: VolunteerProjectCreated) {
        name = volunteerProjectCreated.name
        code = volunteerProjectCreated.code
    }
}
