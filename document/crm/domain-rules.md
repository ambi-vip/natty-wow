# 领域模型开发规范

## 1. 聚合根（Aggregate Root）

### 1.1 基本规范
- 使用`@AggregateRoot`注解标记聚合根类
- 聚合根类必须包含一个`state`参数，类型为对应的`State`类
- 所有命令处理方法必须使用`@OnCommand`注解
- 错误处理方法必须使用`@OnError`注解

### 1.2 命令处理规范
```kotlin
@AggregateRoot
class Project(private val state: ProjectState) {
    @OnCommand
    fun onCreate(command: CreateProject): Mono<ProjectCreated> {
        // 1. 业务规则校验
        // 2. 返回事件
    }

    @OnError
    fun onError(command: CreateProject, error: Throwable): Mono<Void> {
        // 错误处理逻辑
    }
}
```

## 2. 状态（State）

### 2.1 基本规范
- 实现`Identifier`接口
- 所有属性使用`private set`修饰符
- 属性初始值设置合理默认值
- 使用`@OnSourcing`注解处理事件

### 2.2 状态类结构
```kotlin
class ProjectState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var status: ProjectStatus = ProjectStatus.DRAFT
        private set
    
    @OnSourcing
    fun onCreated(event: ProjectCreated) {
        // 更新状态
    }
}
```

## 3. 命令（Command）

### 3.1 基本规范
- 使用`data class`定义命令类
- 创建命令使用`@CreateAggregate`注解
- 所有命令使用`@CommandRoute`注解定义路由
- 使用验证注解确保数据有效性

### 3.2 命令类结构
```kotlin
@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建项目"
)
data class CreateProject(
    @field:NotBlank
    val name: String,
    
    @field:NotNull
    val projectTypeId: String
)
```

## 4. 事件（Event）

### 4.1 基本规范
- 使用`data class`定义事件类
- 事件类名以动词过去式结尾（如：Created, Updated）
- 事件包含必要的状态变更信息
- 事件类与命令类放在同一文件中

### 4.2 事件类结构
```kotlin
data class ProjectCreated(
    val name: String,
    val projectTypeId: String
)
```

## 5. 状态流转规则

### 5.1 基本规范
- 状态变更必须通过事件触发
- 状态流转必须符合业务规则
- 状态变更必须更新`updateTime`
- 创建时必须设置`createTime`

### 5.2 状态流转示例
```kotlin
@OnSourcing
fun onStatusChanged(event: ProjectStatusChanged) {
    status = event.newStatus
    updateTime = LocalDateTime.now()
}
```

## 6. 错误处理

### 6.1 基本规范
- 使用`require`进行业务规则校验
- 错误信息必须清晰明确
- 错误处理方法必须返回`Mono<Void>`
- 错误处理要考虑事务回滚

### 6.2 错误处理示例
```kotlin
@OnError
fun onError(command: CreateProject, error: Throwable): Mono<Void> {
    // 错误处理逻辑
    return Mono.empty()
}
```

## 7. 命名规范

### 7.1 类命名
- 聚合根：使用领域概念名称（如：Project）
- 状态类：聚合根名称 + State（如：ProjectState）
- 命令类：动词 + 聚合根名称（如：CreateProject）
- 事件类：聚合根名称 + 动词过去式（如：ProjectCreated）

### 7.2 方法命名
- 命令处理方法：on + 命令名称（如：onCreate）
- 事件处理方法：on + 事件名称（如：onCreated）
- 错误处理方法：onError

## 8. 包结构规范

### 8.1 基本规范
- 聚合根类放在`domain`模块
- 命令和事件类放在`api`模块
- 按领域概念组织包结构
- 相关类放在同一包下

### 8.2 包结构示例
```
site.weixing.natty
├── api
│   └── cth
│       └── project
│           ├── CreateProject.kt
│           └── UpdateProject.kt
└── domain
    └── cth
        └── project
            ├── Project.kt
            └── ProjectState.kt
``` 

## 9. 事件投影（Event Projection）

### 9.1 基本规范
- 使用`@ProjectionProcessorComponent`注解标记投影处理器类
- 投影处理器类名以`Projector`结尾
- 事件处理方法名以`onEvent`开头
- 使用SLF4J进行日志记录

### 9.2 投影处理器结构
```kotlin
@ProjectionProcessorComponent
class DemoProjector {
    companion object {
        private val log = LoggerFactory.getLogger(DemoProjector::class.java)
    }

    fun onEvent(event: DemoCreated) {
        if (log.isDebugEnabled) {
            log.debug("onEvent: $event")
        }
    }
}
```

### 9.3 投影处理规范
- 每个事件类型对应一个处理方法
- 方法参数类型必须与事件类型匹配
- 使用日志记录事件处理过程
- 避免在投影处理器中执行耗时操作

### 9.4 包结构规范
- 投影处理器类放在`server`模块
- 按领域概念组织包结构
- 相关投影处理器放在同一包下 