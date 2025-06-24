package site.weixing.natty.domain.ums.user

import me.ahoo.wow.infra.prepare.PreparedValue.Companion.toForever
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.domain.ums.account.UsernameIndexValue
import site.weixing.natty.domain.ums.account.UsernamePrepare
import site.weixing.natty.domain.ums.crypto.infra.PasswordEncoder
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.domain.ums.account.UsernamePrepare.Companion.EMAIL_PREFIX
import site.weixing.natty.domain.ums.account.UsernamePrepare.Companion.PHONE_PREFIX
import site.weixing.natty.domain.ums.account.UsernamePrepare.Companion.USERNAME_PREFIX

interface SaveUserSpec {
    fun require(command: CreateUser): Mono<CreateUser>
    fun prepare(command: CreateUser, user: UserState): Mono<Void>
}

@Component
class DefaultSaveUserSpec(
    private val userQueryService: SnapshotQueryService<UserState>,
    private val usernamePrepare: UsernamePrepare,
    private val passwordEncoder: PasswordEncoder
) : SaveUserSpec {



    override fun require(command: CreateUser): Mono<CreateUser> {
        return Mono.`when`(
            validateField(command.username, "username", USERNAME_PREFIX),
            validateField(command.primaryEmail, "email", EMAIL_PREFIX),
            validateField(command.primaryPhone, "phone", PHONE_PREFIX)
        ).thenReturn(command)
    }


    private fun validateField(value: String?, fieldName: String, prefix: String): Mono<Void> {
        if (value == null) {
            return Mono.empty()
        }
        val key = prefix + value
        return usernamePrepare.getValue(key)
            .flatMap { existingUser ->
                if (existingUser != null) {
                    Mono.error(IllegalArgumentException("$fieldName[$value] is already registered."))
                } else {
                    Mono.empty()
                }
            }
    }

    override fun prepare(command: CreateUser, user: UserState): Mono<Void> {
        val encodedPassword = passwordEncoder.encode("123123")
        val usernameIndexValue = UsernameIndexValue(
            userId = user.id,
            password = encodedPassword,
        ).toForever()
        val prepares = mutableListOf<Mono<Boolean>>()
        command.username?.let {
            prepares.add(usernamePrepare.prepare(USERNAME_PREFIX + it, usernameIndexValue))
        }
        command.primaryEmail?.let {
            prepares.add(usernamePrepare.prepare(EMAIL_PREFIX + it, usernameIndexValue))
        }
        command.primaryPhone?.let {
            prepares.add(usernamePrepare.prepare(PHONE_PREFIX + it, usernameIndexValue))
        }
        return Mono.`when`(*prepares.toTypedArray()).then()
    }
}