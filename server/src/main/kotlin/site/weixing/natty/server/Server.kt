package site.weixing.natty.server

import me.ahoo.wow.api.annotation.BoundedContext
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@BoundedContext(name = "natty-wow")
@SpringBootApplication(
    scanBasePackages = ["site.weixing.natty"]
)
@EnableScheduling
class Server

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(Server::class.java), args)
}
