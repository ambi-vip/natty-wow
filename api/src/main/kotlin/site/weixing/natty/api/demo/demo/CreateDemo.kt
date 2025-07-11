package site.weixing.natty.api.demo.demo

import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建Demo"
)
data class CreateDemo(
    @field:NotBlank
    val data: String
)

data class DemoCreated(
    val data: String
)
