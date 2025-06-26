package site.weixing.natty.domain.crm.contact

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.contact.ContactCreated
import site.weixing.natty.api.crm.contact.ContactDeleted
import site.weixing.natty.api.crm.contact.ContactUpdated
import site.weixing.natty.api.crm.contact.CreateContact
import site.weixing.natty.api.crm.contact.DeleteContact
import site.weixing.natty.api.crm.contact.UpdateContact

@AggregateRoot
class Contact(private val state: ContactState) {
    @OnCommand
    fun onCreate(command: CreateContact): ContactCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "联系人名称不能为空" }
        require(command.phone.isNotBlank()) { "联系电话不能为空" }
        require(command.customerId.isNotBlank()) { "客户ID不能为空" }

        // 返回事件
        return ContactCreated(
            name = command.name,
            phone = command.phone,
            email = command.email,
            position = command.position,
            customerId = command.customerId,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateContact): Mono<ContactUpdated> {
        // 业务规则校验
        require(state.name != null) { "联系人不存在" }
        command.name?.let { require(it.isNotBlank()) { "联系人名称不能为空" } }
        // TODO: 更详细的业务规则校验，例如电话号码格式，邮箱格式等

        // 返回事件
        return Mono.just(
            ContactUpdated(
                name = command.name,
                phone = command.phone,
                email = command.email,
                position = command.position,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteContact): Mono<ContactDeleted> {
        // 业务规则校验
        require(state.name != null) { "联系人不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有业务关联的联系人，例如：require(state.status != ContactStatus.ACTIVE) { "活跃联系人不能删除" }

        // 返回事件
        return Mono.just(ContactDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateContact, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateContact command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateContact, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateContact command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteContact, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteContact command: ${error.message}")
        return Mono.empty()
    }
} 
