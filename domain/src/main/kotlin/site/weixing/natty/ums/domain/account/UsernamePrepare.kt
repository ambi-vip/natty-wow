package site.weixing.natty.ums.domain.account

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import reactor.core.publisher.Mono

// 用户名预处理服务，用于防重复
interface UsernamePrepare {  
    fun <T> usingPrepare(  
        key: String,  
        value: UsernameIndexValue,  
        action: (Boolean) -> Mono<T>
    ): Mono<T>
}

// 用户名索引值，用于防重复
data class UsernameIndexValue(
    val userId: String,
    val password: String
)

