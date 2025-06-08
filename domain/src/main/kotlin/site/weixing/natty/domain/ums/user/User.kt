package site.weixing.natty.domain.ums.user

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.ums.api.user.CreateUser
import site.weixing.natty.ums.api.user.UserCreated

@Suppress("unused")
@AggregateRoot
class User(private val state: UserState) {

    @OnCommand
    fun onCreate(command: CreateUser): UserCreated {
        return UserCreated(
            name = command.name,
            accountId = command.accountId,
            email = command.email,
            phone = command.phone,
            avatar = command.avatar
        )
    }
}
