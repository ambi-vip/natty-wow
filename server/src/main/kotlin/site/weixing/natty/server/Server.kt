package site.weixing.natty.server

import me.ahoo.wow.api.annotation.BoundedContext
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import site.weixing.natty.DemoService
import site.weixing.natty.domain.DemoBoundedContext

@BoundedContext(name = DemoService.SERVICE_NAME)
@SpringBootApplication(
    scanBasePackageClasses = [DemoBoundedContext::class, Server::class],
)
class Server

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(Server::class.java), args)
}
