package site.weixing.natty.ums.domain.saga

import me.ahoo.wow.test.SagaVerifier.sagaVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import site.weixing.natty.domain.ums.saga.UserAccountSaga
import site.weixing.natty.api.ums.account.CreateAccount
import site.weixing.natty.ums.api.user.UserCreated

class UserAccountSagaTest {

    @Test
    fun onUserCreated_whenNoAccountId_shouldCreateAccount() {
        val event = UserCreated(
            name = "Test User",
            email = "test@example.com",
            accountId = null,
            phone = "10000000001",
            avatar = ""
        )

        sagaVerifier<UserAccountSaga>()
            .whenEvent(event)
            .expectCommandBody<CreateAccount> {
                assertThat(it.username, equalTo(event.name))
                assertThat(it.email, equalTo(event.email))
                assertThat(it.password, equalTo("changeme"))
            }
            .verify()
    }

//    @Test
//    fun onUserCreated_whenHasAccountId_shouldNotCreateAccount() {
//        sagaVerifier<UserAccountSaga>()
//            .whenEvent(
//                mockk<UserCreated> {
//                    every { accountId } returns "account1"
//                }
//            )
//            .expectNoCommand()
//            .verify()
//    }
}
