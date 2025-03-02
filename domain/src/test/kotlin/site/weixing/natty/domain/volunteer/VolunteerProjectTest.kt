package site.weixing.natty.domain.volunteer

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import site.weixing.natty.api.volunteer.CreateVolunteerProject
import site.weixing.natty.api.volunteer.VolunteerProjectCreated

class VolunteerProjectTest {

    @Test
    fun onCreate() {
        val command = CreateVolunteerProject(
            name = "name",
            code = "code"
        )

        aggregateVerifier<VolunteerProject, VolunteerProjectState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(VolunteerProjectCreated::class.java)
            .expectState {
                assertThat(it.name, equalTo(command.name))
            }
            .verify()
    }
}
