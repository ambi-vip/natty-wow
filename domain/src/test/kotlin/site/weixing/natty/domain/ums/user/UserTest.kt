package site.weixing.natty.domain.ums.user

import io.mockk.every
import io.mockk.mockk
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated
import site.weixing.natty.api.ums.user.UserStatus
import site.weixing.natty.domain.ums.account.UsernameIndexValue

class UserTest {

    val saveUserPrepare = mockk<SaveUserPrepare> {
        every { bindPrepare(any(), any()) } returns Mono.just(UsernameIndexValue(
            userId = "1",
            password = "123456",
            encryptionMethod = ""
        ))
        every { rollback(any(), any()) } returns Mono.empty()
    }

    @Test
    fun onCreate() {
        val command = CreateUser(
            name = "Test User",
            primaryEmail = "test@example.com",
            primaryPhone = "13800138000",
            avatar = "avatar.jpg",
            accountId = null
        )

        aggregateVerifier<User, UserState>()
            .inject(saveUserPrepare)
            .`when`(command)
            .expectNoError()
            .expectEventType(UserCreated::class.java)
            .expectState {
                assertThat(it.name).isEqualTo(command.name)
                assertThat(it.primaryEmail).isEqualTo(command.primaryEmail)
                assertThat(it.primaryPhone).isEqualTo(command.primaryPhone)
                assertThat(it.avatar).isEqualTo(command.avatar)
                assertThat(it.status).isEqualTo(UserStatus.ACTIVE)
            }
            .verify()
    }
}
