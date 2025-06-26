package site.weixing.natty.domain.crm.contact

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.contact.ContactCreated
import site.weixing.natty.api.crm.contact.ContactUpdated
import java.time.LocalDateTime

class ContactState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var phone: String? = null
        private set
    var email: String? = null
        private set
    var position: String? = null
        private set
    var customerId: String? = null
        private set
    var remark: String? = null
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: ContactCreated) {
        name = event.name
        phone = event.phone
        email = event.email
        position = event.position
        customerId = event.customerId
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: ContactUpdated) {
        name = event.name ?: name
        phone = event.phone ?: phone
        email = event.email ?: email
        position = event.position ?: position
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
} 
