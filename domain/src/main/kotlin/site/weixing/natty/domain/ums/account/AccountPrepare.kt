package site.weixing.natty.domain.ums.account

// /**
// * Desc
// * @author ambi
// */
// @Component
// class AccountPrepare(
//    private val prepareKeyFactory: PrepareKeyFactory
// ) : PrepareKey<UsernameIndexValue> {
//
//    private val prepareKey: PrepareKey<UsernameIndexValue> by lazy {
//        prepareKeyFactory.create("username", UsernameIndexValue::class.java)
//    }
//
//    override fun <R> usingPrepare(key: String, value: UsernameIndexValue, then: (Boolean) -> Mono<R>): Mono<R> {
//        return super.usingPrepare(key, value, then)
//    }
// }
