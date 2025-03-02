package site.weixing.natty.domain.volunteer

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.api.volunteer.CreateVolunteerProject
import site.weixing.natty.api.volunteer.VolunteerProjectCreated

/**
 * Desc
 * @author ambi
 */
@Suppress("unused")
@AggregateRoot
class VolunteerProject(private val state: VolunteerProjectState) {

    @OnCommand
    fun onCreate(command: CreateVolunteerProject): VolunteerProjectCreated {
        return VolunteerProjectCreated(
            code = command.code,
            name = command.name
        )
    }
}
