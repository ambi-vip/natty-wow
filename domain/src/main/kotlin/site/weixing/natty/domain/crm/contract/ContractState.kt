package site.weixing.natty.domain.crm.contract

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.contract.ContractCreated
import site.weixing.natty.api.crm.contract.ContractUpdated
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ContractState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var customerId: String? = null
        private set
    var businessId: String? = null
        private set
    var amount: BigDecimal? = null
        private set
    var signDate: LocalDate? = null
        private set
    var startDate: LocalDate? = null
        private set
    var endDate: LocalDate? = null
        private set
    var remark: String? = null
        private set
    var status: ContractStatus = ContractStatus.DRAFT
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: ContractCreated) {
        name = event.name
        customerId = event.customerId
        businessId = event.businessId
        amount = event.amount
        signDate = event.signDate
        startDate = event.startDate
        endDate = event.endDate
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: ContractUpdated) {
        name = event.name ?: name
        amount = event.amount ?: amount
        signDate = event.signDate ?: signDate
        startDate = event.startDate ?: startDate
        endDate = event.endDate ?: endDate
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
}

enum class ContractStatus {
    DRAFT, // 草稿
    APPROVED, // 已审批
    EFFECTIVE, // 生效中
    TERMINATED, // 已终止
    EXPIRED // 已过期
} 
