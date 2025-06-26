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
    fun bindPrepare(command: CreateUser, user: UserState): Mono<CreateUser>
}

@Component
class DefaultSaveUserPrepare(
    private val usernamePrepare: UsernamePrepare,
    private val passwordEncoder: PasswordEncoder
) : SaveUserPrepare {

    override fun bindPrepare(command: CreateUser, user: UserState): Mono<CreateUser> {
        val usernameIndexValue = buildIndexValue(user)
        return Mono.`when`(
            usingPrepareField(command.username, "username", usernameIndexValue),
            usingPrepareField(command.primaryEmail, "email", usernameIndexValue),
            usingPrepareField(command.primaryPhone, "phone", usernameIndexValue)
        ).thenReturn(command)
    }

    private fun buildIndexValue(user: UserState): UsernameIndexValue {
        val encodedPassword = passwordEncoder.encode("123123")
        return UsernameIndexValue(
            userId = user.id,
            password = encodedPassword,
        )
    }

    private fun usingPrepareField(
        index: String?,
        fieldName: String,
        usernameIndexValue: UsernameIndexValue
    ): Mono<Void> {
        if (index == null) {
            return Mono.empty()
        }
        return usernamePrepare.usingPrepare(
            key = index,
            value = usernameIndexValue,
        ) {
            require(it) {
                "$fieldName[$index] is already registered."
            }
            Mono.empty()
        }
    }
}
