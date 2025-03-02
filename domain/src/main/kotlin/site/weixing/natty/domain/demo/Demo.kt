package site.weixing.natty.domain.demo

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.StaticTenantId
import site.weixing.natty.api.demo.CreateDemo
import site.weixing.natty.api.demo.DemoCreated
import site.weixing.natty.api.demo.DemoUpdated
import site.weixing.natty.api.demo.UpdateDemo

@Suppress("unused")
@StaticTenantId
@AggregateRoot
class Demo(private val state: DemoState) {

    @OnCommand
    fun onCreate(command: CreateDemo): DemoCreated {
        return DemoCreated(
            data = command.data,
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateDemo): DemoUpdated {
        return DemoUpdated(
            data = command.data
        )
    }
}
