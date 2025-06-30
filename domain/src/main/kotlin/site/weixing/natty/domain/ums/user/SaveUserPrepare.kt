package site.weixing.natty.domain.ums.user

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.domain.ums.account.UsernameIndexValue
import site.weixing.natty.domain.ums.account.UsernamePrepare
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder

/**
 * bind Prepare
 */
interface SaveUserPrepare {
    fun bindPrepare(command: CreateUser, user: UserState): Mono<UsernameIndexValue>
    fun rollback(command: CreateUser, user: UserState): Mono<Void>
}

@Component
class DefaultSaveUserPrepare(
    private val usernamePrepare: UsernamePrepare,
    private val passwordEncoder: PasswordEncoder
) : SaveUserPrepare {

    override fun bindPrepare(command: CreateUser, user: UserState): Mono<UsernameIndexValue> {
        val usernameIndexValue = buildIndexValue(user)

        // 创建预分配任务映射
        val prepareTasks = mapOf(
            "userName" to command.username?.takeIf { it.isNotBlank() }?.let {
                usernamePrepare.prepare(it, usernameIndexValue)
            },
            "phone" to command.primaryPhone?.takeIf { it.isNotBlank() }?.let {
                usernamePrepare.prepare(it, usernameIndexValue)
            },
            "email" to command.primaryEmail?.takeIf { it.isNotBlank() }?.let {
                usernamePrepare.prepare(it, usernameIndexValue)
            }
        ).filterValues { it != null }.mapValues { it.value!! }

        // 如果没有需要预分配的字段
        if (prepareTasks.isEmpty()) {
            return Mono.just(usernameIndexValue)
        }

        // 并行执行预分配
        return Mono.zip(prepareTasks.values.toList()) { results ->
            prepareTasks.keys.zip(results.map { it as Boolean }).toMap()
        }.flatMap { resultMap ->
            val failedFields = resultMap.filterValues { !it }.keys

            if (failedFields.isEmpty()) {
                // 全部成功
                Mono.just(usernameIndexValue)
            } else {
                // 回滚成功的预分配并返回错误
                rollbackSuccessfulFields(command, resultMap.filterValues { it }.keys)
                    .then(Mono.error(createConflictError(failedFields)))
            }
        }
    }

    private fun rollbackSuccessfulFields(command: CreateUser, successFields: Set<String>): Mono<Void> {
        val rollbacks = successFields.mapNotNull { field ->
            when (field) {
                "userName" -> command.username?.let { usernamePrepare.rollback(it) }
                "phone" -> command.primaryPhone?.let { usernamePrepare.rollback(it) }
                "email" -> command.primaryEmail?.let { usernamePrepare.rollback(it) }
                else -> null
            }
        }

        return if (rollbacks.isNotEmpty()) {
            Mono.zip(rollbacks) { }.then()
        } else {
            Mono.empty()
        }
    }

    private fun createConflictError(failedFields: Set<String>): IllegalStateException {
        val conflictMessages = failedFields.map { field ->
            when (field) {
                "userName" -> "用户名已被占用"
                "phone" -> "手机号已被占用"
                "email" -> "邮箱已被占用"
                else -> "${field}字段冲突"
            }
        }

        val errorMessage = conflictMessages.joinToString("，")
        return IllegalStateException(errorMessage)
    }

    override fun rollback(
        command: CreateUser,
        user: UserState
    ): Mono<Void> {
        TODO("Not yet implemented")
    }

    private fun buildIndexValue(user: UserState): UsernameIndexValue {
        val encodedPassword = passwordEncoder.encode("123123")
        return UsernameIndexValue(
            userId = user.id,
            password = encodedPassword,
            encryptionMethod = "bcrypt",
        )
    }
}
