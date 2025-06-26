package site.weixing.natty.domain.crm.followup

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.followup.FollowupCreated
import site.weixing.natty.api.crm.followup.FollowupUpdated
import java.time.LocalDateTime

class FollowupState(override val id: String) : Identifier {
    var targetId: String? = null
        private set
    var targetType: String? = null
        private set
    var content: String? = null
        private set
    var followupTime: LocalDateTime? = null
        private set
    var followupMethod: String? = null
        private set
    var remark: String? = null
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: FollowupCreated) {
        targetId = event.targetId
        targetType = event.targetType
        content = event.content
        followupTime = event.followupTime
        followupMethod = event.followupMethod
        remark = event.remark
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: FollowupUpdated) {
        content = event.content ?: content
        followupTime = event.followupTime ?: followupTime
        followupMethod = event.followupMethod ?: followupMethod
        remark = event.remark ?: remark
        updateTime = LocalDateTime.now()
    }
} 
