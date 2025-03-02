package site.weixing.natty.server.auth

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Desc
 * @author ambi
 */
@RestController
class HelloController {

    @GetMapping("/hello")
    fun hello() :String {
        return "Hello";
    }


}