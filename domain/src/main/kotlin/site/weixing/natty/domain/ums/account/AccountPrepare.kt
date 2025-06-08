package site.weixing.natty.domain.ums.account

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

///**
// * Desc
// * @author ambi
// */
//@Component
//class AccountPrepare(
//    private val prepareKeyFactory: PrepareKeyFactory
//) : PrepareKey<UsernameIndexValue> {
//
//    private val prepareKey: PrepareKey<UsernameIndexValue> by lazy {
//        prepareKeyFactory.create("username", UsernameIndexValue::class.java)
//    }
//
//    override fun <R> usingPrepare(key: String, value: UsernameIndexValue, then: (Boolean) -> Mono<R>): Mono<R> {
//        return super.usingPrepare(key, value, then)
//    }
//}