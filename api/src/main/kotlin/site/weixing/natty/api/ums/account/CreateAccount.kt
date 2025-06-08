package site.weixing.natty.api.ums.account

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建账户"
)
data class CreateAccount(

    @field:NotBlank
    val userId: String,

    @field:NotBlank
    val username: String,

    @field:NotBlank
    val phone: String,

    @field:Email
    val email: String,

    val password: String

)

data class AccountCreated(
    val username: String,
    val email: String,
    val phone: String?
)
