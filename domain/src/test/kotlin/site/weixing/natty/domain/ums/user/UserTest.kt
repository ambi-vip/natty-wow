package site.weixing.natty.domain.ums.user

import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import site.weixing.natty.api.ums.user.CreateUser
import site.weixing.natty.api.ums.user.UserCreated

class UserTest {

    @Test
    fun onCreate() {
        val command = CreateUser(
            name = "Test User",
            email = "test@example.com",
            phone = "13800138000",
            avatar = "avatar.jpg",
            accountId = null
        )

        aggregateVerifier<User, UserState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(UserCreated::class.java)
            .expectState {
                assertThat(it.name, equalTo(command.name))
                assertThat(it.primaryEmail, equalTo(command.email))
                assertThat(it.primaryPhone, equalTo(command.phone))
                assertThat(it.avatar, equalTo(command.avatar))
                assertThat(it.accountId, equalTo(command.accountId))
                assertThat(it.status, equalTo(UserStatus.ACTIVE))
            }
            .verify()
    }

    @Test
    fun onCreate_withExistingAccount() {
        val accountId = GlobalIdGenerator.generateAsString()
        val command = CreateUser(
            name = "Test User",
            email = "test@example.com",
            phone = "13800138000",
            avatar = "avatar.jpg",
            accountId = accountId
        )

        aggregateVerifier<User, UserState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(UserCreated::class.java)
            .expectState {
                assertThat(it.name, equalTo(command.name))
                assertThat(it.primaryEmail, equalTo(command.email))
                assertThat(it.primaryPhone, equalTo(command.phone))
                assertThat(it.avatar, equalTo(command.avatar))
                assertThat(it.accountId, equalTo(accountId))
                assertThat(it.status, equalTo(UserStatus.ACTIVE))
            }
            .verify()
    }
}
