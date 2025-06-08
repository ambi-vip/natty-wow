package site.weixing.natty.domain.demo

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import site.weixing.natty.api.demo.demo.CreateDemo
import site.weixing.natty.api.demo.demo.DemoCreated
import site.weixing.natty.api.demo.demo.DemoUpdated
import site.weixing.natty.api.demo.demo.UpdateDemo

@Suppress("unused")
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
        if (command.data == "updated") {
            throw RuntimeException("xxxx")
        }
        return DemoUpdated(
            data = command.data
        )
    }
}
