package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.annotation.Name
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import site.weixing.natty.domain.ums.account.UsernamePrepare
import site.weixing.natty.ums.api.user.CreateUser

interface SaveUserSpec {
    fun require(user: CreateUser): Mono<CreateUser>
}

@Name("createOrderSpec")
class DefaultCreateOrderSpec(
    private val userQueryService: SnapshotQueryService<UserState>,
    private val usernamePrepare: UsernamePrepare
) : SaveUserSpec {

    override fun require(user: CreateUser): Mono<CreateUser> {

    }


    private fun saveUserName(user: CreateUser): Mono<Void> {
        return Mono.defer {
            user.username?.let { username ->
                usernamePrepare.getValue(username)
                    .flatMap { existingValue ->
                        // 如果 usernamePrepare.getValue(username) 有值，抛出异常
                       return
                    }
                    .switchIfEmpty {
                        // 如果没有值，继续执行保存逻辑
                        // saveUsernameToDatabase(username)
                        Mono.empty() // 返回一个空的 Mono，表示操作成功
                    }
            } ?: Mono.empty() // 如果 username 为 null，则返回一个空的 Mono
        }
    }

}