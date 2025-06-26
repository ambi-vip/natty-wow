package site.weixing.natty.domain.crm.statistics

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.statistics.CreateStatistic
import site.weixing.natty.api.crm.statistics.DeleteStatistic
import site.weixing.natty.api.crm.statistics.StatisticCreated
import site.weixing.natty.api.crm.statistics.StatisticDeleted
import site.weixing.natty.api.crm.statistics.StatisticUpdated
import site.weixing.natty.api.crm.statistics.UpdateStatistic

@AggregateRoot
class Statistic(private val state: StatisticState) {
    @OnCommand
    fun onCreate(command: CreateStatistic): StatisticCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "统计名称不能为空" }
        require(command.periodType.isNotBlank()) { "统计周期类型不能为空" }
        require(command.periodValue.isNotBlank()) { "统计周期值不能为空" }
        require(command.value.signum() >= 0) { "统计值不能为负数" }

        // 返回事件
        return StatisticCreated(
            name = command.name,
            periodType = command.periodType,
            periodValue = command.periodValue,
            value = command.value,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateStatistic): Mono<StatisticUpdated> {
        // 业务规则校验
        require(state.name != null) { "统计数据不存在" }
        command.value?.let { require(it.signum() >= 0) { "统计值不能为负数" } }
        // TODO: 更详细的业务规则校验，例如名称、周期类型和周期值的组合唯一性等

        // 返回事件
        return Mono.just(
            StatisticUpdated(
                name = command.name,
                periodType = command.periodType,
                periodValue = command.periodValue,
                value = command.value,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteStatistic): Mono<StatisticDeleted> {
        // 业务规则校验
        require(state.name != null) { "统计数据不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除被其他模块引用的统计数据

        // 返回事件
        return Mono.just(StatisticDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateStatistic, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateStatistic command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateStatistic, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateStatistic command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteStatistic, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteStatistic command: ${error.message}")
        return Mono.empty()
    }
} 
