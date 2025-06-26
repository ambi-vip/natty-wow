// package site.weixing.natty.auth
//
// import me.ahoo.cosec.token.TokenCompositeAuthentication
// import me.ahoo.wow.command.CommandBus
// import me.ahoo.wow.command.CommandGateway
// import me.ahoo.wow.command.toCommandMessage
// import org.springframework.web.bind.annotation.PostMapping
// import org.springframework.web.bind.annotation.RequestBody
// import org.springframework.web.bind.annotation.RequestMapping
// import org.springframework.web.bind.annotation.RestController
// import reactor.core.publisher.Mono
// import site.weixing.natty.auth.domain.commands.Login
// import site.weixing.natty.auth.domain.commands.RefreshToken
// import site.weixing.natty.auth.domain.events.LoggedIn
// import site.weixing.natty.auth.domain.events.TokenRefreshed
//
// @RestController
// @RequestMapping("/auth")
// class AuthController(
//    private val tokenCompositeAuthentication: TokenCompositeAuthentication,
//    private val authCommandBus: CommandGateway
// ) {
//
//    @PostMapping("/login")
//    fun login(@RequestBody loginRequest: LoginRequest): Mono<LoginResponse> {
//        val command = Login(
//            username = loginRequest.username,
//            password = loginRequest.password
//        )
//
//        return authCommandBus.send(command.toCommandMessage())
//            .map { event ->
//                when (event) {
//                    is LoggedIn -> LoginResponse(
//                        accessToken = event.token,
//                        refreshToken = event.refreshToken,
//                        expiresIn = event.expiresAt.toEpochMilli()
//                    )
//                    else -> throw IllegalStateException("Unexpected event type")
//                }
//            }
//    }
//
//    @PostMapping("/refresh")
//    fun refreshToken(@RequestBody refreshRequest: RefreshTokenRequest): Mono<LoginResponse> {
//        val command = RefreshToken(
//            refreshToken = refreshRequest.refreshToken
//        )
//
//        return authCommandBus.send(command.toCommandMessage())
//            .map { event ->
//                when (event) {
//                    is TokenRefreshed -> LoginResponse(
//                        accessToken = event.newToken,
//                        refreshToken = event.newRefreshToken,
//                        expiresIn = event.expiresAt.toEpochMilli()
//                    )
//                    else -> throw IllegalStateException("Unexpected event type")
//                }
//            }
//    }
// }
//
// data class LoginRequest(
//    val username: String,
//    val password: String
// )
//
// data class LoginResponse(
//    val accessToken: String,
//    val refreshToken: String,
//    val expiresIn: Long
// )
//
// data class RefreshTokenRequest(
//    val refreshToken: String
// )
