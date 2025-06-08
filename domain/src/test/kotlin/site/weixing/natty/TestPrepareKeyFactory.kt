package site.weixing.natty

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import me.ahoo.wow.infra.prepare.PreparedValue
import reactor.core.publisher.Mono

/**
 * TestPrepareKeyFactory
 * @author ambi
 */
object TestPrepareKeyFactory {

    fun create(): PrepareKeyFactory {
        return object : PrepareKeyFactory {
            override fun <T : Any> create(name: String, valueClass: Class<T>): PrepareKey<T> {
                return object : PrepareKey<T> {
                    override fun <R> usingPrepare(key: String, value: T, then: (Boolean) -> Mono<R>): Mono<R> {
                        return then(true)
                    }

                    override val name: String
                        get() = name

                    override fun getValue(key: String): Mono<PreparedValue<T>> {
                        return Mono.empty()
                    }

                    override fun rollback(key: String): Mono<Boolean> {
                        return Mono.just(true)
                    }

                    override fun rollback(key: String, value: T): Mono<Boolean> {
                        return Mono.just(true)
                    }

                    override fun reprepare(key: String, value: PreparedValue<T>): Mono<Boolean> {
                        return Mono.just(true)
                    }

                    override fun reprepare(key: String, oldValue: T, newValue: PreparedValue<T>): Mono<Boolean> {
                        return Mono.just(true)
                    }

                    override fun prepare(key: String, value: PreparedValue<T>): Mono<Boolean> {
                        return Mono.just(true)
                    }
                }
            }
        }
    }

}