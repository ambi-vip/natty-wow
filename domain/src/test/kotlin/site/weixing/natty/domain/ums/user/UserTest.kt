package site.weixing.natty.domain.ums.user

import io.mockk.mockk
import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.test.aggregate.VerifiedStage
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated
import site.weixing.natty.api.ums.user.UserStatus

class UserTest {

    class MockSaveSpec() : SaveUserSpec {
        override fun require(command: CreateUser): Mono<CreateUser> {
            return Mono.just(command)
        }

        override fun prepare(
            command: CreateUser,
            user: UserState
        ): Mono<Void> {
            return Mono.empty()
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
            .inject(MockSaveSpec())
            .`when`(command)
            .expectNoError()
            .expectEventType(UserCreated::class.java)
            .expectState {
                assertThat(it.name, equalTo(command.name))
                assertThat(it.primaryEmail, equalTo(command.primaryEmail))
                assertThat(it.primaryPhone, equalTo(command.primaryPhone))
                assertThat(it.avatar, equalTo(command.avatar))
                assertThat(it.accountId, equalTo(command.accountId))
                assertThat(it.status, equalTo(UserStatus.ACTIVE))
            }
            .verify()
    }


}
