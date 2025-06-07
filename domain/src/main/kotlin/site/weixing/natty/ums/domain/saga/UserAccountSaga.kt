package site.weixing.natty.ums.domain.saga

import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.api.annotation.Retry
import me.ahoo.wow.api.event.DomainEvent
import me.ahoo.wow.spring.stereotype.StatelessSagaComponent
import org.slf4j.LoggerFactory
import site.weixing.natty.ums.api.account.CreateAccount
import site.weixing.natty.ums.api.user.UserCreated

@StatelessSagaComponent
class UserAccountSaga {
    companion object {
        private val log = LoggerFactory.getLogger(UserAccountSaga::class.java)
    }

    @OnEvent
    @Retry
    fun onUserCreated(event: DomainEvent<UserCreated>): CreateAccount {
        val userCreated = event.body

        log.debug("Creating account for user: ${event.aggregateId.id}")

        val createAccount = CreateAccount(
            userId = event.aggregateId.id,
            username = userCreated.name,
            phone = userCreated.phone ?: "",
            email = userCreated.email ?: "",
            password = "changeme" // 默认密码，需要用户首次登录修改
        )
        // 1: 事件处理 命令，失败反馈到 onUserCreated
//        return commandGateway.sendAndWaitForSnapshot(createAccount.toCommandMessage(ownerId = event.ownerId))
//            .then()
        // 2: 在 聚合根中定义 OnError 触发补偿
        return createAccount;
    }
}
