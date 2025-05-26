package site.weixing.natty.ums.domain.saga

import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.api.annotation.StatelessSaga
import me.ahoo.wow.api.event.DomainEvent
import me.ahoo.wow.command.factory.CommandBuilder
import me.ahoo.wow.command.factory.CommandBuilder.Companion.commandBuilder
import org.slf4j.LoggerFactory
import site.weixing.natty.ums.api.account.CreateAccount
import site.weixing.natty.ums.api.user.UserCreated

@StatelessSaga
class UserAccountSaga {
    companion object {
        private val log = LoggerFactory.getLogger(UserAccountSaga::class.java)
    }

    @OnEvent
    fun onUserCreated(event: DomainEvent<UserCreated>): CommandBuilder? {
        val userCreated = event.body
        if (userCreated.accountId != null) {
            return null
        }

        log.debug("Creating account for user: ${event.aggregateId.id}")
        return CreateAccount(
            userId = event.aggregateId.id,
            username = userCreated.name,
            phone = userCreated.phone ?: "",
            email = userCreated.email ?: "",
            password = "changeme" // 默认密码，需要用户首次登录修改
        ).commandBuilder()
            .aggregateId(event.ownerId)
    }
}
