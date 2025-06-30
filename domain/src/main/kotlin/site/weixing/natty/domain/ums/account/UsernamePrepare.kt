package site.weixing.natty.domain.ums.account

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import me.ahoo.wow.infra.prepare.PreparedValue
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

// 用户名预处理服务，用于防重复
@Component
class UsernamePrepare(
    private val prepareKeyFactory: PrepareKeyFactory
) : PrepareKey<UsernameIndexValue> by prepareKeyFactory.create(
    "username",
    UsernameIndexValue::class.java
) {
    fun rePrepare(
        oldKey: String,
        newKey: String,
        newValue: PreparedValue<UsernameIndexValue>
    ): Mono<Boolean> {
        require(oldKey != newKey) {
            "oldKey must not be equals to newKey. oldKey:[$oldKey]"
        }
        return usingPrepare(newKey, newValue) { prepared ->
            if (!prepared) {
                return@usingPrepare Mono.just(false)
            }
            rollback(oldKey).doOnNext {
                if (!it) {
                    throw IllegalStateException(
                        "Re prepare - Rollback failed. newKey:[$newKey] oldKey:[$oldKey]"
                    )
                }
            }
        }
    }

    fun rePrepare(oldKey: String, newKey: String): Mono<Boolean> {
        return getValue(oldKey)
            .flatMap { preparedValue ->
                reprepare(oldKey, preparedValue.value, newKey, preparedValue.value)
            }
            .switchIfEmpty(Mono.just(false))
    }
}

// 用户名索引值，用于防重复
data class UsernameIndexValue(
    // 用户id
    val userId: String,
    // 可能没有密码
    val password: String?,
    // 加密方法
    val encryptionMethod: String?,
)
