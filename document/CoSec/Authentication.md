根据您的要求，我将整理 CoSec 项目的使用和对接方法到 MD 文件中。

# CoSec 项目使用和对接指南

## 项目简介

CoSec 是一个基于 RBAC 和策略的多租户响应式安全框架 [1](#2-0) 。该框架提供了完整的认证和授权解决方案，支持 JWT 令牌、策略引擎和多种 Web 框架集成。

## 核心架构

### 主要模块

CoSec 采用模块化设计，包含以下核心模块：

- **cosec-api**: 核心 API 定义
- **cosec-core**: 核心实现
- **cosec-jwt**: JWT 令牌支持
- **cosec-webflux**: WebFlux 集成
- **cosec-webmvc**: WebMVC 集成
- **cosec-spring-boot-starter**: Spring Boot 自动配置

### 认证流程 [2](#2-1)

### 授权流程 [3](#2-2)

## 快速开始

### 1. 依赖配置

在您的 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation("me.ahoo.cosec:cosec-spring-boot-starter:${cosecVersion}")
    implementation("me.ahoo.cosec:cosec-webflux:${cosecVersion}")
}
```

### 2. 基础配置

在 `application.yaml` 中配置 CoSec：

```yaml
cosec:
  authentication:
    enabled: true
  jwt:
    algorithm: hmac256
    secret: your-secret-key-at-least-32-characters-long
  authorization:
    enabled: true
``` [4](#2-3) 

## WebFlux 项目对接

### 1. 自定义认证实现

```kotlin
@Component
class UsernamePasswordAuthentication : Authentication<UsernamePasswordCredentials, CoSecPrincipal> {
    
    override fun authenticate(credentials: UsernamePasswordCredentials): Mono<CoSecPrincipal> {
        return validateUser(credentials.username, credentials.password)
            .map { user ->
                SimplePrincipal(
                    id = user.id,
                    attributes = mapOf(
                        "username" to user.username,
                        "email" to user.email
                    )
                )
            }
    }
    
    private fun validateUser(username: String, password: String): Mono<User> {
        // 实现您的用户验证逻辑
    }
}
```

### 2. 注册认证提供者

```kotlin
@Configuration
class AuthenticationConfiguration {
    
    @PostConstruct
    fun registerAuthentications(
        usernamePasswordAuthentication: UsernamePasswordAuthentication
    ) {
        DefaultAuthenticationProvider.register(
            UsernamePasswordCredentials::class.java,
            usernamePasswordAuthentication
        )
    }
}
```

### 3. 登录控制器

```kotlin
@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenCompositeAuthentication: TokenCompositeAuthentication
) {
    
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): Mono<LoginResponse> {
        val credentials = UsernamePasswordCredentials(
            username = loginRequest.username,
            password = loginRequest.password
        )
        
        return tokenCompositeAuthentication.authenticate(credentials)
            .map { tokenResult ->
                LoginResponse(
                    accessToken = tokenResult.accessToken,
                    refreshToken = tokenResult.refreshToken,
                    expiresIn = tokenResult.expiresIn
                )
            }
    }
}
```

### 4. 安全过滤器

CoSec 自动配置 `ReactiveAuthorizationFilter` 来处理请求的安全上下文解析和授权 [5](#2-4) 。

### 5. 获取当前用户信息

在业务代码中获取当前登录用户：

```kotlin
@RestController
class UserController {
    
    @GetMapping("/me")
    fun getCurrentUser(): Mono<UserInfo> {
        return Mono.deferContextual { contextView ->
            val securityContext = contextView.getSecurityContext()
            val principal = securityContext.principal
            
            Mono.just(UserInfo(
                id = principal.id,
                username = principal.attributes["username"] ?: ""
            ))
        }
    }
}
``` [6](#2-5) 

## 策略配置

### 策略示例

CoSec 支持灵活的策略配置，以下是一个策略示例： [7](#2-6) 

### 自定义 ActionMatcher

如需自定义动作匹配器，可以参考以下实现： [8](#2-7) 

### 自定义 ConditionMatcher

如需自定义条件匹配器，可以参考以下实现： [9](#2-8) 

## 社会化认证

CoSec 支持多种社会化认证方式，包括微信、支付宝、钉钉等 [10](#2-9) 。

## 客户端集成

### 登录请求

```javascript
const loginResponse = await fetch('/auth/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        username: 'your-username',
        password: 'your-password'
    })
});

const { accessToken } = await loginResponse.json();
```

### 携带令牌访问

```javascript
const apiResponse = await fetch('/api/protected-resource', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
```

## 监控和观测

CoSec 集成了 OpenTelemetry 支持，遵循 OpenTelemetry 规范 [11](#2-10) 。

## 部署

项目提供了 Docker 部署支持 [12](#2-11) ，支持多平台构建。

## Notes

CoSec 的权限策略设计参考了 AWS IAM [13](#2-12) ，提供了强大而灵活的授权机制。框架支持多租户架构，适用于各种规模的应用系统。

Wiki pages you might want to explore:
- [Architecture (Ahoo-Wang/CoSec)](/wiki/Ahoo-Wang/CoSec#2)