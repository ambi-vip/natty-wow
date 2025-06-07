package site.weixing.natty.server.ums

import jakarta.annotation.PostConstruct
import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.ums.domain.account.UsernameIndexValue
import site.weixing.natty.ums.domain.account.UsernamePrepare

@Component
class DefaultUsernamePrepare(
    private val prepareKeyFactory: PrepareKeyFactory
) : UsernamePrepare {

    private lateinit var usernamePrepareKey: PrepareKey<UsernameIndexValue>

   @PostConstruct
   fun init() {
       usernamePrepareKey = prepareKeyFactory.create("username", UsernameIndexValue::class.java)
   }

    override fun <T> usingPrepare(key: String, value: UsernameIndexValue, action: (Boolean) -> Mono<T>): Mono<T> {
        return usernamePrepareKey.usingPrepare(key, value, action)
    }
}