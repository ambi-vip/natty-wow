package site.weixing.natty.domain.crm.business

import io.swagger.v3.oas.annotations.tags.Tag
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.business.BusinessCreated
import site.weixing.natty.api.crm.business.BusinessDeleted
import site.weixing.natty.api.crm.business.BusinessUpdated
import site.weixing.natty.api.crm.business.CreateBusiness
import site.weixing.natty.api.crm.business.DeleteBusiness
import site.weixing.natty.api.crm.business.UpdateBusiness

@AggregateRoot
@Tag(name = "customer")
class Business(private val state: BusinessState) {
    @OnCommand
    fun onCreate(command: CreateBusiness): BusinessCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "商机名称不能为空" }
        require(command.customerId.isNotBlank()) { "客户ID不能为空" }
        require(command.expectedRevenue.signum() > 0) { "预期收入必须大于0" }

        // 返回事件
        return BusinessCreated(
            name = command.name,
            customerId = command.customerId,
            expectedRevenue = command.expectedRevenue,
            closeDate = command.closeDate,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateBusiness): Mono<BusinessUpdated> {
        // 业务规则校验
        require(state.name != null) { "商机不存在" }
        command.expectedRevenue?.let { require(it.signum() > 0) { "预期收入必须大于0" } }
        // TODO: 更详细的业务规则校验，例如名称的唯一性，商机状态的流转（例如，已赢单或已输单的商机不能修改）等

        // 返回事件
        return Mono.just(
            BusinessUpdated(
                name = command.name,
                expectedRevenue = command.expectedRevenue,
                closeDate = command.closeDate,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteBusiness): Mono<BusinessDeleted> {
        // 业务规则校验
        require(state.name != null) { "商机不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有合同或应收的商机（例如：require(state.status != BusinessStatus.WON) { "已赢单的商机不能删除" }）

        // 返回事件
        return Mono.just(BusinessDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateBusiness, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateBusiness command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateBusiness, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateBusiness command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteBusiness, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteBusiness command: ${error.message}")
        return Mono.empty()
    }
} 