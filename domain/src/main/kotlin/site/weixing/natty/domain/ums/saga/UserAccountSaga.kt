package site.weixing.natty.domain.ums.saga

import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.api.annotation.Retry
import me.ahoo.wow.api.event.DomainEvent
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.factory.CommandBuilder
import me.ahoo.wow.command.toCommandMessage
import me.ahoo.wow.command.wait.WaitingFor
import me.ahoo.wow.spring.stereotype.StatelessSagaComponent
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import site.weixing.natty.api.ums.account.CreateAccount
import site.weixing.natty.ums.api.user.UserCreated

@StatelessSagaComponent
class UserAccountSaga(
    private val commandGateway: CommandGateway
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserAccountSaga::class.java)
    }

    @OnEvent
    @Retry
    fun onUserCreated(event: DomainEvent<UserCreated>): Mono<Void> {
        val userCreated = event.body


        log.debug("Creating account for user: ${event.aggregateId.id}")

        val createAccount = CreateAccount(
            userId = event.aggregateId.id,
            username = userCreated.name,
            phone = userCreated.phone ?: "",
            email = userCreated.email ?: "",
            password = "changeme" // 默认密码，需要用户首次登录修改
        )
        // 同步 CreateAccount 失败进行事件记录
        return commandGateway.sendAndWait(
            createAccount.toCommandMessage(),
            WaitingFor.processed()
        ).then()
    }
}
