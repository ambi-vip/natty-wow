package site.weixing.natty.domain.crm.business

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.business.BusinessCreated
import site.weixing.natty.api.crm.business.BusinessUpdated
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class BusinessState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var customerId: String? = null
        private set
    var expectedRevenue: BigDecimal? = null
        private set
    var closeDate: LocalDate? = null
        private set
    var remark: String? = null
        private set
    var status: BusinessStatus = BusinessStatus.PROSPECT
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: BusinessCreated) {
        name = event.name
        customerId = event.customerId
        expectedRevenue = event.expectedRevenue
        closeDate = event.closeDate
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: BusinessUpdated) {
        name = event.name ?: name
        expectedRevenue = event.expectedRevenue ?: expectedRevenue
        closeDate = event.closeDate ?: closeDate
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
}

enum class BusinessStatus {
    PROSPECT, // 潜在商机
    NEGOTIATION, // 谈判中
    WON, // 已赢单
    LOST, // 已输单
    CANCELED // 已取消
}
