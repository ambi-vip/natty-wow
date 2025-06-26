package site.weixing.natty.domain.ums.saga

import org.junit.jupiter.api.Test
import site.weixing.natty.api.ums.user.UserCreated

class UserAccountSagaTest {

    @Test
    fun onUserCreated_whenNoAccountId_shouldCreateAccount() {
        val event = UserCreated(
            name = "Test User",
            primaryEmail = "test@example.com",
            accountId = null,
            primaryPhone = "10000000001",
            avatar = "",
            username = "123"
        )

//        sagaVerifier<UserAccountSaga>()
//            .whenEvent(event)
//            .expectCommandBody<CreateAccount> {
//                assertThat(it.username, equalTo(event.name))
//                assertThat(it.email, equalTo(event.primaryEmail))
//                assertThat(it.password, equalTo("changeme"))
//            }
//            .verify()
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
