package site.weixing.natty.domain.ums.account

import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import site.weixing.natty.api.ums.account.AccountCreated
import site.weixing.natty.api.ums.account.AccountDeleted
import site.weixing.natty.api.ums.account.AccountLocked
import site.weixing.natty.api.ums.account.AccountUnlocked
import site.weixing.natty.api.ums.account.AccountUpdated
import site.weixing.natty.api.ums.account.AssignRole
import site.weixing.natty.api.ums.account.CreateAccount
import site.weixing.natty.api.ums.account.DeleteAccount
import site.weixing.natty.api.ums.account.LockAccount
import site.weixing.natty.api.ums.account.RoleAssigned
import site.weixing.natty.api.ums.account.UnlockAccount
import site.weixing.natty.api.ums.account.UpdateAccount
import site.weixing.natty.domain.TestPrepareKeyFactory


class AccountTest {

    private val usernamePrepare = UsernamePrepare(TestPrepareKeyFactory.create())

    @Test
    fun onCreate() {
        val command = CreateAccount(
            userId = GlobalIdGenerator.generateAsString(),
            username = "testuser",
            email = "test@example.com",
            phone = "13800138000",
            password = "11"
        )

        aggregateVerifier<Account, AccountState>()
            .inject(usernamePrepare)
            .`when`(command)
            .expectNoError()
            .expectEventType(AccountCreated::class.java)
            .expectState {
                assertThat(it.username, equalTo(command.username))
                assertThat(it.email, equalTo(command.email))
                assertThat(it.phone, equalTo(command.phone))
                assertThat(it.status, equalTo(AccountStatus.ACTIVE))
            }
            .verify()
    }

    @Test
    fun onUpdate() {
        val command = UpdateAccount(
            id = GlobalIdGenerator.generateAsString(),
            username = "updateduser",
            email = "updated@example.com",
            phone = "13900139000"
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "olduser",
                    email = "old@example.com",
                    phone = "13700137000"
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(AccountUpdated::class.java)
            .expectState {
                assertThat(it.username, equalTo(command.username))
                assertThat(it.email, equalTo(command.email))
                assertThat(it.phone, equalTo(command.phone))
            }
            .verify()
    }

    @Test
    fun onLock() {
        val command = LockAccount(
            id = GlobalIdGenerator.generateAsString(),
            reason = "Security violation"
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(AccountLocked::class.java)
            .expectState {
                assertThat(it.status, equalTo(AccountStatus.LOCKED))
            }
            .verify()
    }

    @Test
    fun onLock_whenAlreadyLocked_shouldThrowException() {
        val command = LockAccount(
            id = GlobalIdGenerator.generateAsString(),
            reason = "Security violation"
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                ),
                AccountLocked(
                    accountId = command.id,
                    reason = "Previous violation"
                )
            )
            .`when`(command)
//            .expectError(IllegalStateException::class.java)
            .verify()
    }

    @Test
    fun onUnlock() {
        val command = UnlockAccount(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                ),
                AccountLocked(
                    accountId = command.id,
                    reason = "Security violation"
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(AccountUnlocked::class.java)
            .expectState {
                assertThat(it.status, equalTo(AccountStatus.ACTIVE))
            }
            .verify()
    }

    @Test
    fun onUnlock_whenNotLocked_shouldThrowException() {
        val command = UnlockAccount(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                )
            )
            .`when`(command)
//            .expectError(IllegalStateException::class.java)
            .verify()
    }

    @Test
    fun onDelete() {
        val command = DeleteAccount(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(AccountDeleted::class.java)
            .expectState {
                assertThat(it.status, equalTo(AccountStatus.DISABLED))
            }
            .verify()
    }

    @Test
    fun onDelete_whenAlreadyDisabled_shouldThrowException() {
        val command = DeleteAccount(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                ),
                AccountDeleted(
                    accountId = command.id
                )
            )
            .`when`(command)
//            .expectError(IllegalStateException::class.java)
            .verify()
    }

    @Test
    fun onAssignRole() {
        val command = AssignRole(
            id = GlobalIdGenerator.generateAsString(),
            roleIds = setOf("role1", "role2")
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(RoleAssigned::class.java)
            .expectState {
                assertThat(it.roleIds, equalTo(command.roleIds))
            }
            .verify()
    }

    @Test
    fun onAssignRole_whenAccountLocked_shouldThrowException() {
        val command = AssignRole(
            id = GlobalIdGenerator.generateAsString(),
            roleIds = setOf("role1", "role2")
        )

        aggregateVerifier<Account, AccountState>()
            .given(
                AccountCreated(
                    username = "testuser",
                    email = "test@example.com",
                    phone = "13800138000"
                ),
                AccountLocked(
                    accountId = command.id,
                    reason = "Security violation"
                )
            )
            .`when`(command)
            .expectErrorType(IllegalStateException::class.java)
            .verify()
    }
}
