package site.weixing.natty.domain.crm.receivable

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.receivable.CreateReceivable
import site.weixing.natty.api.crm.receivable.DeleteReceivable
import site.weixing.natty.api.crm.receivable.MarkReceivableAsPaid
import site.weixing.natty.api.crm.receivable.ReceivableCreated
import site.weixing.natty.api.crm.receivable.ReceivableDeleted
import site.weixing.natty.api.crm.receivable.ReceivablePaid
import site.weixing.natty.api.crm.receivable.ReceivableUpdated
import site.weixing.natty.api.crm.receivable.UpdateReceivable

@AggregateRoot
class Receivable(private val state: ReceivableState) {
    @OnCommand
    fun onCreate(command: CreateReceivable): ReceivableCreated {
        // 业务规则校验
        require(command.contractId.isNotBlank()) { "合同ID不能为空" }
        require(command.amount.signum() > 0) { "应收金额必须大于0" }
        require(command.dueDate.isAfter(java.time.LocalDate.MIN)) { "到期日期无效" }

        // 返回事件
        return ReceivableCreated(
            contractId = command.contractId,
            amount = command.amount,
            dueDate = command.dueDate,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateReceivable): Mono<ReceivableUpdated> {
        // 业务规则校验
        require(state.contractId != null) { "应收不存在" }
        require(state.status == ReceivableStatus.PENDING) { "已支付的应收不能修改" }
        command.amount?.let { require(it.signum() > 0) { "应收金额必须大于0" } }
        command.dueDate?.let { require(it.isAfter(java.time.LocalDate.MIN)) { "到期日期无效" } }
        // TODO: 更详细的业务规则校验

        // 返回事件
        return Mono.just(
            ReceivableUpdated(
                amount = command.amount,
                dueDate = command.dueDate,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteReceivable): Mono<ReceivableDeleted> {
        // 业务规则校验
        require(state.contractId != null) { "应收不存在" }
        require(state.status == ReceivableStatus.PENDING) { "已支付的应收不能删除" }

        // 返回事件
        return Mono.just(ReceivableDeleted(state.id))
    }

    @OnCommand
    fun onMarkAsPaid(command: MarkReceivableAsPaid): Mono<ReceivablePaid> {
        // 业务规则校验
        require(state.contractId != null) { "应收不存在" }
        require(state.status == ReceivableStatus.PENDING) { "应收已支付或不可标记为已支付" }

        // 返回事件
        return Mono.just(ReceivablePaid(state.id))
    }

    @OnError
    fun onError(command: CreateReceivable, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateReceivable command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateReceivable, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateReceivable command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteReceivable, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteReceivable command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: MarkReceivableAsPaid, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing MarkReceivableAsPaid command: ${error.message}")
        return Mono.empty()
    }
}
