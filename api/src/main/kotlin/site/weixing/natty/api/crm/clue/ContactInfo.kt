package site.weixing.natty.api.crm.clue

import io.swagger.v3.oas.annotations.media.Schema
import me.ahoo.wow.api.annotation.ValueObject

@ValueObject
@Schema(title = "联系方式信息")
data class ContactInfo(

    val customerName: String? = null,
    val customerId: String? = null,

    val mobile: String? = null,
    val telephone: String? = null,
    val qq: String? = null,
    val wechat: String? = null,
    val email: String? = null,

    var areaId: String? = null,
    var address: String? = null,

) 