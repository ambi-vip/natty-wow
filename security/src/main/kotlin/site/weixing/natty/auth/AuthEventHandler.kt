package site.weixing.natty.auth

import me.ahoo.wow.api.annotation.OnEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import site.weixing.natty.api.auth.AuthAuthenticated
import site.weixing.natty.api.auth.TokenRefreshed

@Component
class AuthEventHandler {
    
    companion object {
        private val log = LoggerFactory.getLogger(AuthEventHandler::class.java)
    }
    
    @OnEvent
    fun onLoggedIn(event: AuthAuthenticated) {
        if (log.isDebugEnabled) {
            log.debug("User logged in: userId={}", event.accountId)
        }
        // 处理登录成功事件
        // 例如：更新用户会话、发送通知等
    }

    @OnEvent
    fun onTokenRefreshed(event: TokenRefreshed) {
        if (log.isDebugEnabled) {
            log.debug("Token refreshed: userId={}", event.accountId)
        }
        // 处理令牌刷新事件
        // 例如：更新令牌记录、记录日志等
    }
} 