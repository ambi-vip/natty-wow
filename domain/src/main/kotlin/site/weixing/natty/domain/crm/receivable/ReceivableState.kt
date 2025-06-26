package site.weixing.natty.domain.crm.receivable

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.receivable.ReceivableCreated
import site.weixing.natty.api.crm.receivable.ReceivablePaid
import site.weixing.natty.api.crm.receivable.ReceivableUpdated
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ReceivableState(override val id: String) : Identifier {
    var contractId: String? = null
        private set
    var amount: BigDecimal? = null
        private set
    var dueDate: LocalDate? = null
        private set
    var remark: String? = null
        private set
    var status: ReceivableStatus = ReceivableStatus.PENDING
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: ReceivableCreated) {
        contractId = event.contractId
        amount = event.amount
        dueDate = event.dueDate
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: ReceivableUpdated) {
        amount = event.amount ?: amount
        dueDate = event.dueDate ?: dueDate
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }

    @OnSourcing
    fun onPaid(event: ReceivablePaid) {
        status = ReceivableStatus.PAID
        updateTime = LocalDateTime.now()
    }
}

enum class ReceivableStatus {
    PENDING, // 待支付
    PAID, // 已支付
    OVERDUE // 已逾期
}
