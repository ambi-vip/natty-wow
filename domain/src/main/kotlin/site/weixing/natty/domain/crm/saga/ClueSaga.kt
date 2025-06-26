package site.weixing.natty.domain.crm.saga

import me.ahoo.wow.api.annotation.OnEvent
import me.ahoo.wow.api.event.DomainEvent
import me.ahoo.wow.command.CommandGateway
import me.ahoo.wow.command.factory.CommandBuilder
import me.ahoo.wow.command.factory.CommandBuilder.Companion.commandBuilder
import me.ahoo.wow.spring.stereotype.StatelessSagaComponent
import org.slf4j.LoggerFactory
import site.weixing.natty.api.crm.clue.ClueTransformed
import site.weixing.natty.api.crm.customer.CreateCustomer
import site.weixing.natty.api.crm.customer.CustomerCreated
import site.weixing.natty.domain.crm.clue.ClueState

/**
 * Desc
 * @author ambi
 */
@StatelessSagaComponent
class ClueSaga(
    private val commandGateway: CommandGateway
) {

    companion object {
        private val log = LoggerFactory.getLogger(ClueSaga::class.java)
    }

    @OnEvent
    fun onClueTransformed(event: ClueTransformed, state: ClueState): CommandBuilder? {
        val contactInfo = state.contactInfo ?: return null

        // TODO 针对信息 判断是否需要新增用户

        // 1: 事件处理 命令，失败反馈到 onUserCreated
        return CreateCustomer(
            name = contactInfo.customerName ?: "",
            phone = contactInfo.mobile ?: "",
            email = contactInfo.email ?: "",
            address = contactInfo.address ?: "",
            remark = state.remark ?: "",
        ).commandBuilder()
    }

    @OnEvent
    fun onCustomerCrated(event: DomainEvent<CustomerCreated>): CommandBuilder? {
        val id = event.id
        val source = event.body.source ?: return null
        if (!"CULD".equals(source)) return null

        return null
    }
}
