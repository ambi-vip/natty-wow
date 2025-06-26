package site.weixing.natty.domain.crm.customer

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.customer.CustomerCreated
import site.weixing.natty.api.crm.customer.CustomerUpdated
import java.time.LocalDateTime

class CustomerState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var phone: String? = null
        private set
    var email: String? = null
        private set
    var address: String? = null
        private set
    var source: String? = null
        private set
    var remark: String? = null
        private set
    var status: CustomerStatus = CustomerStatus.POTENTIAL
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: CustomerCreated) {
        name = event.name
        phone = event.phone
        email = event.email
        address = event.address
        remark = event.remark
        source = event.source
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: CustomerUpdated) {
        name = event.name ?: name
        phone = event.phone ?: phone
        email = event.email ?: email
        address = event.address ?: address
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
}

enum class CustomerStatus {
    POTENTIAL, // 潜在客户
    DEAL, // 成交客户
    LOST // 客户流失
}
