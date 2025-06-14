package site.weixing.natty.domain.crm.statistics

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.statistics.StatisticCreated
import site.weixing.natty.api.crm.statistics.StatisticUpdated
import java.math.BigDecimal
import java.time.LocalDateTime

class StatisticState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var periodType: String? = null
        private set
    var periodValue: String? = null
        private set
    var value: BigDecimal? = null
        private set
    var remark: String? = null
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: StatisticCreated) {
        name = event.name
        periodType = event.periodType
        periodValue = event.periodValue
        value = event.value
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: StatisticUpdated) {
        name = event.name ?: name
        periodType = event.periodType ?: periodType
        periodValue = event.periodValue ?: periodValue
        value = event.value ?: value
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
} 