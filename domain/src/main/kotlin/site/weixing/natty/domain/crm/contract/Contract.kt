package site.weixing.natty.domain.crm.contract

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.contract.ContractCreated
import site.weixing.natty.api.crm.contract.ContractDeleted
import site.weixing.natty.api.crm.contract.ContractUpdated
import site.weixing.natty.api.crm.contract.CreateContract
import site.weixing.natty.api.crm.contract.DeleteContract
import site.weixing.natty.api.crm.contract.UpdateContract

@AggregateRoot
class Contract(private val state: ContractState) {
    @OnCommand
    fun onCreate(command: CreateContract): ContractCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "合同名称不能为空" }
        require(command.customerId.isNotBlank()) { "客户ID不能为空" }
        require(command.businessId.isNotBlank()) { "商机ID不能为空" }
        require(command.amount.signum() > 0) { "合同金额必须大于0" }
        require(command.signDate.isAfter(java.time.LocalDate.MIN)) { "签订日期无效" }
        command.startDate?.let { require(!it.isAfter(command.endDate)) { "合同开始日期不能晚于结束日期" } }

        // 返回事件
        return ContractCreated(
            name = command.name,
            customerId = command.customerId,
            businessId = command.businessId,
            amount = command.amount,
            signDate = command.signDate,
            startDate = command.startDate,
            endDate = command.endDate,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateContract): Mono<ContractUpdated> {
        // 业务规则校验
        require(state.name != null) { "合同不存在" }
        command.amount?.let { require(it.signum() > 0) { "合同金额必须大于0" } }
        command.signDate?.let { require(it.isAfter(java.time.LocalDate.MIN)) { "签订日期无效" } }
        // TODO: 更详细的业务规则校验，例如日期有效性，状态流转（例如，已生效的合同不能随意修改金额）等

        // 返回事件
        return Mono.just(
            ContractUpdated(
                name = command.name,
                amount = command.amount,
                signDate = command.signDate,
                startDate = command.startDate,
                endDate = command.endDate,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteContract): Mono<ContractDeleted> {
        // 业务规则校验
        require(state.name != null) { "合同不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有应收或已生效的合同（例如：require(state.status != ContractStatus.EFFECTIVE) { "生效中的合同不能删除" }）

        // 返回事件
        return Mono.just(ContractDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateContract, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateContract command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateContract, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateContract command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteContract, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteContract command: ${error.message}")
        return Mono.empty()
    }
} 