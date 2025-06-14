package site.weixing.natty.domain.ums.permission

import me.ahoo.wow.test.aggregate.`when`
import me.ahoo.wow.test.aggregateVerifier
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import site.weixing.natty.domain.ums.permission.Permission
import site.weixing.natty.domain.ums.permission.PermissionState
import site.weixing.natty.domain.ums.permission.PermissionStatus
import site.weixing.natty.ums.api.permission.CreatePermission
import site.weixing.natty.ums.api.permission.PermissionCreated
import site.weixing.natty.ums.api.permission.PermissionType

class PermissionTest {

    @Test
    fun onCreate() {
        val command = CreatePermission(
            code = "user:create",
            name = "Create User",
            description = "Permission to create new users",
            type = PermissionType.OPERATION
        )

        aggregateVerifier<Permission, PermissionState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(PermissionCreated::class.java)
            .expectState {
                assertThat(it.code, equalTo(command.code))
                assertThat(it.name, equalTo(command.name))
                assertThat(it.description, equalTo(command.description))
                assertThat(it.type, equalTo(command.type))
                assertThat(it.status, equalTo(PermissionStatus.ACTIVE))
            }
            .verify()
    }

    @Test
    fun onCreate_withMenuType() {
        val command = CreatePermission(
            code = "user:management",
            name = "User Management",
            description = "Menu for user management",
            type = PermissionType.MENU
        )

        aggregateVerifier<Permission, PermissionState>()
            .`when`(command)
            .expectNoError()
            .expectEventType(PermissionCreated::class.java)
            .expectState {
                assertThat(it.code, equalTo(command.code))
                assertThat(it.name, equalTo(command.name))
                assertThat(it.description, equalTo(command.description))
                assertThat(it.type, equalTo(command.type))
                assertThat(it.status, equalTo(PermissionStatus.ACTIVE))
            }
            .verify()
    }
}
