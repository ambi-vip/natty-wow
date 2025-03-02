package site.weixing.natty.api.volunteer

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

/**
 * Desc
 * @author ambi
 */
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建志愿者项目"
)
data class CreateVolunteerProject(
    @field:NotBlank
    val name: String,
    val code: String,
)

data class VolunteerProjectCreated(
    val name: String,
    val code: String,
)
