package site.weixing.natty.domain.auth

import jakarta.annotation.Resource
import org.casbin.casdoor.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI


/**
 * Desc
 * @author ambi
 */
@Controller
@RequestMapping("/auth")
class AuthController {

    @Resource
    private val casdoorAuthService: AuthService? = null

    @GetMapping("/toLogin")
    fun toLogin(): Mono<ResponseEntity<Void>> {
        val signinUrl = casdoorAuthService?.getSigninUrl("http://localhost:8080/auth/login")
        return if (signinUrl != null) {
            // 正确的重定向响应
            Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(URI.create(signinUrl)).build())
        } else {
            // 如果没有找到 URL，返回 400 错误
            Mono.just(ResponseEntity.badRequest().body(null))
        }
    }

    @RequestMapping("login")
    fun login(
        @RequestParam code: String,
        @RequestParam state: String,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return Mono.defer {
            val token = casdoorAuthService?.getOAuthToken(code, state)
            val user = casdoorAuthService?.parseJwtToken(token)

            // 使用 WebSession 来存储用户信息
            exchange.attributes["casdoorUser"] = user

            // 返回 302 重定向响应
            Mono.just(ResponseEntity.status(302).header("Location", "/").build<Void>())
        }.onErrorResume { e: Throwable ->
            e.printStackTrace()
            // 返回 400 错误响应
            Mono.just(ResponseEntity.badRequest().build())
        }
    }


}