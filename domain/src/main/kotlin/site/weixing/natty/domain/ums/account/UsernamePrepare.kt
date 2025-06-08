package site.weixing.natty.domain.ums.account

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component

// 用户名预处理服务，用于防重复
@Component
class UsernamePrepare(
    private val prepareKeyFactory: PrepareKeyFactory
) : PrepareKey<UsernameIndexValue> by prepareKeyFactory.create("username", UsernameIndexValue::class.java)


// 用户名索引值，用于防重复
data class UsernameIndexValue(
    val userId: String,
    val password: String
)

