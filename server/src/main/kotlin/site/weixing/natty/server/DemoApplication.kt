package site.weixing.natty.server

import me.ahoo.wow.api.annotation.BoundedContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import site.weixing.natty.DemoService
import site.weixing.natty.domain.DemoBoundedContext

@BoundedContext(name = DemoService.SERVICE_NAME)
@SpringBootApplication(
    scanBasePackageClasses = [DemoBoundedContext::class, DemoApplication::class],
)
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
