package site.weixing.natty.domain.crm.operatelog

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.operatelog.OperateLogCreated
import java.time.LocalDateTime

class OperateLogState(override val id: String) : Identifier {
    var operatorId: String? = null
        private set
    var operatorName: String? = null
        private set
    var operation: String? = null
        private set
    var targetType: String? = null
        private set
    var targetId: String? = null
        private set
    var remark: String? = null
        private set
    var createTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: OperateLogCreated) {
        operatorId = event.operatorId
        operatorName = event.operatorName
        operation = event.operation
        targetType = event.targetType
        targetId = event.targetId
        remark = event.remark
        createTime = LocalDateTime.now()
    }
} 