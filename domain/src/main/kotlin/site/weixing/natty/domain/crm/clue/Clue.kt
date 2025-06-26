package site.weixing.natty.domain.crm.clue

import io.swagger.v3.oas.annotations.tags.Tag
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import me.ahoo.wow.api.annotation.StaticTenantId
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.clue.AddClueFollowUp
import site.weixing.natty.api.crm.clue.AddClueTransformRecord
import site.weixing.natty.api.crm.clue.ClueCreated
import site.weixing.natty.api.crm.clue.ClueDeleted
import site.weixing.natty.api.crm.clue.ClueFollowUpAdded
import site.weixing.natty.api.crm.clue.ClueTransferred
import site.weixing.natty.api.crm.clue.ClueTransformRecordAdded
import site.weixing.natty.api.crm.clue.ClueTransformed
import site.weixing.natty.api.crm.clue.ClueUpdated
import site.weixing.natty.api.crm.clue.CreateClue
import site.weixing.natty.api.crm.clue.DeleteClue
import site.weixing.natty.api.crm.clue.TransferClue
import site.weixing.natty.api.crm.clue.TransformClue
import site.weixing.natty.api.crm.clue.UpdateClue
import java.util.UUID

@AggregateRoot
@StaticTenantId
@Tag(name = "customer")
class Clue(private val state: ClueState) {
    @OnCommand
    fun onCreate(command: CreateClue): ClueCreated {
        // 业务规则校验
        // TODO: 1: 校验负责人是否存在
        // TODO: 2: 校验手机号码、邮箱等唯一性

        // 返回事件
        return ClueCreated(
            name = command.name,
            ownerUserId = command.ownerUserId,
            contactInfo = command.contactInfo,
            areaId = command.areaId,
            detailAddress = command.detailAddress,
            industryId = command.industryId,
            level = command.level,
            source = command.source,
            remark = command.remark
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateClue): ClueUpdated {
        // 业务规则校验
        // TODO: 1: 校验负责人是否存在
        // TODO: 2: 校验手机号码、邮箱等唯一性
        // TODO: 3: 校验更新字段的有效性

        // 返回事件
        return ClueUpdated(
            name = command.name,
            ownerUserId = command.ownerUserId,
            contactInfo = command.contactInfo,
            areaId = command.areaId,
            detailAddress = command.detailAddress,
            industryId = command.industryId,
            level = command.level,
            source = command.source,
            remark = command.remark
        )
    }

    @OnCommand
    fun onTransform(command: TransformClue): ClueTransformed {
        // 业务规则校验
        require(!state.transformStatus) { "线索已转化" }
        // TODO: 1: 校验客户ID是否存在
        // TODO: 2: 校验转化类型的有效性

        // 返回事件
        return ClueTransformed(
            id = command.id,
            customerId = command.customerId,
            transformType = command.transformType,
            transformReason = command.transformReason
        )
    }

    @OnCommand
    fun onTransfer(command: TransferClue): ClueTransferred {
        // 业务规则校验
        require(state.ownerUserId != command.newOwnerUserId) { "新负责人与原负责人相同" }
        // TODO: 1: 校验新负责人ID是否存在

        // 返回事件
        return ClueTransferred(
            id = command.id,
            oldOwnerUserId = state.ownerUserId,
            newOwnerUserId = command.newOwnerUserId
        )
    }

    @OnCommand
    fun onAddFollowUp(command: AddClueFollowUp): ClueFollowUpAdded {
        // 业务规则校验
        require(command.content.isNotBlank()) { "跟进内容不能为空" }
        require(command.nextTime.isAfter(java.time.LocalDateTime.now().minusMinutes(1))) { "下次跟进时间不能是过去时间" }

        // 返回事件
        return ClueFollowUpAdded(
            clueId = command.clueId,
            followUpId = UUID.randomUUID().toString(), // 生成唯一ID
            content = command.content,
            nextTime = command.nextTime
        )
    }

    @OnCommand
    fun onAddTransformRecord(command: AddClueTransformRecord): ClueTransformRecordAdded {
        // 业务规则校验
        require(state.name != null) { "线索不存在" }
        require(command.customerId.isNotBlank()) { "客户ID不能为空" }
        // TODO: 1: 校验转化记录的有效性

        // 返回事件
        return ClueTransformRecordAdded(
            clueId = command.clueId,
            recordId = UUID.randomUUID().toString(), // 生成唯一ID
            customerId = command.customerId,
            transformType = command.transformType,
            transformReason = command.transformReason
        )
    }

    @OnCommand
    fun onDelete(command: DeleteClue): ClueDeleted {
        // 业务规则校验
        require(state.name != null) { "线索不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有客户或商机关联的线索
        require(!state.transformStatus) { "已转化的线索不能删除" }

        // 返回事件
        return ClueDeleted(command.id)
    }

    @OnError
    fun onError(command: CreateClue, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateClue command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateClue, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateClue command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: TransformClue, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing TransformClue command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: TransferClue, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing TransferClue command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: AddClueFollowUp, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing AddClueFollowUp command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: AddClueTransformRecord, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing AddClueTransformRecord command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteClue, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteClue command: ${error.message}")
        return Mono.empty()
    }
}
