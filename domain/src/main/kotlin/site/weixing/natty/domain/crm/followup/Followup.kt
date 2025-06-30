package site.weixing.natty.domain.crm.followup

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.followup.CreateFollowup
import site.weixing.natty.api.crm.followup.DeleteFollowup
import site.weixing.natty.api.crm.followup.FollowupCreated
import site.weixing.natty.api.crm.followup.FollowupDeleted
import site.weixing.natty.api.crm.followup.FollowupUpdated
import site.weixing.natty.api.crm.followup.UpdateFollowup

@AggregateRoot
class Followup(private val state: FollowupState) {

    @OnCommand
    fun onCreate(command: CreateFollowup): FollowupCreated {
        // 业务规则校验
        require(command.content.isNotBlank()) { "跟进内容不能为空" }
        require(command.followupMethod.isNotBlank()) { "跟进方式不能为空" }

        // 返回事件
        return FollowupCreated(
            targetId = command.targetId,
            targetType = command.targetType,
            content = command.content,
            followupTime = command.followupTime,
            followupMethod = command.followupMethod,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateFollowup): Mono<FollowupUpdated> {
        // 业务规则校验
        require(state.targetId != null) { "跟进记录不存在" }
        command.followupTime?.let {
            require(
                it.isBefore(java.time.LocalDateTime.now().plusMinutes(1))
            ) { "跟进时间不能是未来时间" }
        }
        // TODO: 更详细的业务规则校验，例如跟进内容的有效性，跟进方式的有效性等

        // 返回事件
        return Mono.just(
            FollowupUpdated(
                content = command.content,
                followupTime = command.followupTime,
                followupMethod = command.followupMethod,
                remark = command.remark
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteFollowup): Mono<FollowupDeleted> {
        // 业务规则校验
        require(state.targetId != null) { "跟进记录不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除与重要事件关联的跟进记录

        // 返回事件
        return Mono.just(FollowupDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateFollowup, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateFollowup command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateFollowup, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateFollowup command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteFollowup, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteFollowup command: ${error.message}")
        return Mono.empty()
    }
}
