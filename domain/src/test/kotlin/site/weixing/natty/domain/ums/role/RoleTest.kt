package site.weixing.natty.domain.ums.role

import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import site.weixing.natty.domain.ums.role.Role
import site.weixing.natty.domain.ums.role.RoleState
import site.weixing.natty.domain.ums.role.RoleStatus
import site.weixing.natty.ums.api.role.CreateRole
import site.weixing.natty.ums.api.role.DeleteRole
import site.weixing.natty.ums.api.role.RoleCreated
import site.weixing.natty.ums.api.role.RoleDeleted
import site.weixing.natty.ums.api.role.RoleUpdated
import site.weixing.natty.ums.api.role.UpdateRole

class RoleTest {

    @Test
    fun onCreate() {
        val command = CreateRole(
            name = "Admin",
            description = "Administrator role",
            permissions = setOf("user:create", "user:update", "user:delete")
        )

        aggregateVerifier<Role, RoleState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(RoleCreated::class.java)
            .expectState {
                assertThat(it.name).isEqualTo(command.name)
                assertThat(it.description).isEqualTo(command.description)
                assertThat(it.permissions).isEqualTo(command.permissions)
                assertThat(it.status).isEqualTo(RoleStatus.ACTIVE)

            }
            .verify()
    }

    @Test
    fun onUpdate() {
        val command = UpdateRole(
            id = GlobalIdGenerator.generateAsString(),
            name = "Updated Admin",
            description = "Updated Administrator role",
            permissions = setOf("user:create", "user:update", "user:delete", "user:view")
        )

        aggregateVerifier<Role, RoleState>()
            .given(
                RoleCreated(
                    name = "Admin",
                    description = "Administrator role",
                    permissions = setOf("user:create", "user:update", "user:delete")
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(RoleUpdated::class.java)
            .expectState {
                assertThat(it.name).isEqualTo(command.name)
                assertThat(it.description).isEqualTo(command.description)
                assertThat(it.permissions).isEqualTo(command.permissions)
                assertThat(it.status).isEqualTo(RoleStatus.ACTIVE)
            }
            .verify()
    }

    @Test
    fun onUpdate_whenDisabled_shouldThrowException() {
        val command = UpdateRole(
            id = GlobalIdGenerator.generateAsString(),
            name = "Updated Admin",
            description = "Updated Administrator role",
            permissions = setOf("user:create", "user:update", "user:delete", "user:view")
        )

        aggregateVerifier<Role, RoleState>()
            .given(
                RoleCreated(
                    name = "Admin",
                    description = "Administrator role",
                    permissions = setOf("user:create", "user:update", "user:delete")
                ),
                RoleDeleted(
                    roleId = command.id
                )
            )
            .`when`(command)
            .expectErrorType(IllegalStateException::class.java)
            .verify()
    }

    @Test
    fun onDelete() {
        val command = DeleteRole(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Role, RoleState>()
            .given(
                RoleCreated(
                    name = "Admin",
                    description = "Administrator role",
                    permissions = setOf("user:create", "user:update", "user:delete")
                )
            )
            .`when`(command)
            .expectNoError()
            .expectEventType(RoleDeleted::class.java)
            .expectState {
                assertThat(it.status).isEqualTo(RoleStatus.DISABLED)
            }
            .verify()
    }

    @Test
    fun onDelete_whenAlreadyDisabled_shouldThrowException() {
        val command = DeleteRole(
            id = GlobalIdGenerator.generateAsString()
        )

        aggregateVerifier<Role, RoleState>()
            .given(
                RoleCreated(
                    name = "Admin",
                    description = "Administrator role",
                    permissions = setOf("user:create", "user:update", "user:delete")
                ),
                RoleDeleted(
                    roleId = command.id
                )
            )
            .`when`(command)
            .expectErrorType(IllegalStateException::class.java)
            .verify()
    }
}
