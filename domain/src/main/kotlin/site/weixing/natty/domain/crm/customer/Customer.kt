package site.weixing.natty.domain.crm.customer

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.customer.CreateCustomer
import site.weixing.natty.api.crm.customer.CustomerCreated
import site.weixing.natty.api.crm.customer.CustomerDeleted
import site.weixing.natty.api.crm.customer.CustomerUpdated
import site.weixing.natty.api.crm.customer.DeleteCustomer
import site.weixing.natty.api.crm.customer.UpdateCustomer

@AggregateRoot
class Customer(private val state: CustomerState) {
    @OnCommand
    fun onCreate(command: CreateCustomer): CustomerCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "客户名称不能为空" }

        // 返回事件
        return CustomerCreated(
            name = command.name,
            phone = command.phone,
            email = command.email,
            address = command.address,
            remark = command.remark,
            source = command.source
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateCustomer): Mono<CustomerUpdated> {
        // 业务规则校验
        require(state.name != null) { "客户不存在" }
        command.name?.let { require(it.isNotBlank()) { "客户名称不能为空" } }
        // TODO: 更详细的业务规则校验，例如电话号码格式，邮箱格式，名称唯一性等

        // 返回事件
        return Mono.just(
            CustomerUpdated(
                name = command.name,
                phone = command.phone,
                email = command.email,
                address = command.address,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteCustomer): Mono<CustomerDeleted> {
        // 业务规则校验
        require(state.name != null) { "客户不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有商机、合同、联系人的客户（例如：require(state.status != CustomerStatus.DEAL) { "已成交客户不能删除" }）

        // 返回事件
        return Mono.just(CustomerDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateCustomer, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateCustomer command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateCustomer, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateCustomer command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteCustomer, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteCustomer command: ${error.message}")
        return Mono.empty()
    }
}
