package site.weixing.natty.api.ums.account

import me.ahoo.wow.api.exception.ErrorInfo

/**
 * Desc
 * @author ambi
 */
data class AccountError(
    val reason: String? = null
) : ErrorInfo {
    override val errorCode: String
        get() = ""

    override val errorMsg: String
        get() = "2"
}
