package site.weixing.natty.domain.ums.user

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated
import site.weixing.natty.api.ums.user.UserStatus

class UserTest {

    class MockSavePrepare : SaveUserPrepare {
        override fun bindPrepare(command: CreateUser, user: UserState): Mono<CreateUser> {
            return Mono.just(command)
        }
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
            .inject(MockSavePrepare())
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
