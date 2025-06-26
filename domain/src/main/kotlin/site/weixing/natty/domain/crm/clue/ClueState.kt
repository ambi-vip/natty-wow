package site.weixing.natty.domain.crm.clue

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.clue.ClueCreated
import site.weixing.natty.api.crm.clue.ClueFollowUpAdded
import site.weixing.natty.api.crm.clue.ClueTransferred
import site.weixing.natty.api.crm.clue.ClueTransformRecordAdded
import site.weixing.natty.api.crm.clue.ClueTransformed
import site.weixing.natty.api.crm.clue.ClueUpdated
import site.weixing.natty.api.crm.clue.ContactInfo
import java.time.LocalDateTime

class ClueState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var ownerUserId: String? = null
        private set
    var contactInfo: ContactInfo? = null
        private set
    var industryId: String? = null
        private set
    var level: String? = null
        private set
    var source: String? = null
        private set
    var remark: String? = null
        private set
    var status: ClueStatus = ClueStatus.NEW
        private set
    var transformStatus: Boolean = false
        private set
    var customerId: String? = null
        private set
    var followUpStatus: Boolean = false
        private set
    var contactLastTime: LocalDateTime? = null
        private set
    var contactLastContent: String? = null
        private set
    var contactNextTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: ClueCreated) {
        name = event.name
        ownerUserId = event.ownerUserId
        contactInfo = event.contactInfo
        industryId = event.industryId
        level = event.level
        source = event.source
        remark = event.remark
        status = ClueStatus.NEW
        transformStatus = false
        followUpStatus = false
        contactLastTime = null
        contactLastContent = null
        contactNextTime = null
    }

    @OnSourcing
    fun onUpdated(event: ClueUpdated) {
        event.name?.let { name = it }
        event.ownerUserId?.let { ownerUserId = it }
        event.contactInfo?.let { contactInfo = it }
        event.industryId?.let { industryId = it }
        event.level?.let { level = it }
        event.source?.let { source = it }
        event.remark?.let { remark = it }
    }

    @OnSourcing
    fun onTransformed(event: ClueTransformed) {
        transformStatus = true
        customerId = event.customerId
        status = ClueStatus.QUALIFIED
    }

    @OnSourcing
    fun onTransferred(event: ClueTransferred) {
        ownerUserId = event.newOwnerUserId
    }

    @OnSourcing
    fun onFollowUpAdded(event: ClueFollowUpAdded) {
        followUpStatus = true
        contactLastTime = event.nextTime
        contactLastContent = event.content
        contactNextTime = event.nextTime
    }

    @OnSourcing
    fun onTransformRecordAdded(event: ClueTransformRecordAdded) {
        // This event primarily records the transformation. The state is already updated by onTransformed.
        // No direct state changes here, but keeping the method for completeness if future needs arise.
    }
}

enum class ClueStatus {
    NEW, // 新建
    FOLLOWING, // 跟进中
    QUALIFIED, // 已转化
    INVALID // 无效
}
