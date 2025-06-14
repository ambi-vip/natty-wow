package site.weixing.natty.domain.crm.operatelog

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.operatelog.CreateOperateLog
import site.weixing.natty.api.crm.operatelog.OperateLogCreated

@AggregateRoot
class OperateLog(private val state: OperateLogState) {
    @OnCommand
    fun onCreate(command: CreateOperateLog): OperateLogCreated {
        // 业务规则校验
        require(command.operatorId.isNotBlank()) { "操作人ID不能为空" }
        require(command.operatorName.isNotBlank()) { "操作人姓名不能为空" }
        require(command.operation.isNotBlank()) { "操作内容不能为空" }
        require(command.targetType.isNotBlank()) { "操作目标类型不能为空" }
        require(command.targetId.isNotBlank()) { "操作目标ID不能为空" }

        // 返回事件
        return OperateLogCreated(
            operatorId = command.operatorId,
            operatorName = command.operatorName,
            operation = command.operation,
            targetType = command.targetType,
            targetId = command.targetId,
            remark = command.remark
        )
    }

    @OnError
    fun onError(command: CreateOperateLog, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateOperateLog command: ${error.message}")
        return Mono.empty()
    }
} 