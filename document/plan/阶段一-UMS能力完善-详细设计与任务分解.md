# 阶段一：通用UMS能力完善——详细设计与任务分解

## 一、目标
- 完善账户、用户、角色、权限、会话等通用聚合与API，支撑后续所有CRM子模块的统一认证、授权、权限校验。
- 实现基础的认证、授权、权限校验、用户管理等能力。
- 完善UMS相关聚合、Saga、事件补偿、单元测试。

## 二、领域建模与API梳理

### 1. 账户（Account）
- 命令：创建账户、修改凭证、锁定/解锁、登录、修改邮箱/手机号、分配角色/权限等
- 事件：账户已创建、凭证已修改、账户已锁定/解锁、登录成功/失败、邮箱/手机号已变更等
- 状态：账户基本信息、状态、凭证、关联用户、角色、权限等

### 2. 用户（User）
- 命令：创建用户、更新用户、完善资料、关联/解绑账户、设置主账户等
- 事件：用户已创建、已更新、资料已完善、账户已关联/解绑、主账户已变更等
- 状态：用户基本信息、关联账户、主账户等

### 3. 角色（Role）
- 命令：创建角色、更新角色、分配权限等
- 事件：角色已创建、已更新、权限已分配等
- 状态：角色信息、权限列表

### 4. 权限（Permission）
- 命令：创建权限、更新权限
- 事件：权限已创建、已更新
- 状态：权限信息

### 5. 会话/认证（Session/Auth）
- 命令：创建会话、刷新会话、撤销会话、登录、登出等
- 事件：会话已创建/刷新/撤销/过期、登录成功/失败等
- 状态：会话信息、状态、关联账户/用户等

### 6. Saga/补偿
- 用户注册自动创建账户Saga
- 账户与用户信息同步Saga
- 事件补偿机制

## 领域模型交互规范

遵循 DDD 和事件驱动架构原则，UMS 领域模型之间的交互应主要通过**发布和监听领域事件**来完成。禁止领域模型之间直接调用命令或查询，以保持模型解耦和边界清晰。Saga 负责协调跨聚合的业务流程，监听相关事件并发送命令。

## 三、具体任务分解

1. 梳理并补全所有UMS相关命令、事件、API对象（api模块）
2. 完善/补全所有UMS聚合根、状态对象、命令处理、事件溯源（domain模块）
3. 实现/完善UMS相关Saga（如注册自动建账户、信息同步等）
4. 实现/完善认证、授权、权限校验逻辑（server模块）
5. 实现/完善UMS相关的投影、查询接口（server模块）
6. 编写UMS相关聚合、Saga单元测试，保证主流程80%以上覆盖率
7. 配置并验证事件补偿、失败重试、监控等机制
8. 编写/完善相关文档，**文档编写应参考 `document/guide` 文件夹下的风格，尤其是 `query.md` 的 Markdown 格式、代码块、以及 `::: code-group` 等排版方式，以提供清晰易读的功能介绍和代码示例。**

## 四、输出物
- 完整的UMS领域模型、API、聚合、Saga、投影、查询、测试代码
- 事件补偿与监控配置
- 详细开发文档与接口文档

---

> 下一步将依次推进上述任务，优先补全命令/事件/聚合/状态/测试等基础能力，逐步完善Saga、补偿、监控等高级能力。每完成一部分会同步输出到文档和代码中。 