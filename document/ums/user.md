# 用户聚合模型文档

## 1. 功能概述

用户（User）聚合是 UMS（用户管理系统）领域的核心聚合，负责用户的全生命周期管理，包括用户的创建、信息维护、状态变更、密码管理、档案与自定义数据维护等。该聚合与账号、权限、组织等模块紧密关联，是身份认证与授权的基础。

## 2. 功能需求

### 2.1 基础功能

#### 2.1.1 用户创建
- 支持创建新用户
- 必填信息包括：
  - 用户名
  - 账号ID
  - 主邮箱/主手机号
- 可选信息包括：
  - 姓名
  - 头像
  - 其他自定义数据

#### 2.1.2 用户信息维护
- 支持更新用户基础信息（姓名、邮箱、手机号、头像等）
- 支持更新用户档案（昵称、简介、网站、性别、生日、地区、地址等）
- 支持更新自定义数据

#### 2.1.3 用户状态管理
- 支持激活、禁用等状态切换
- 记录状态变更历史
- 禁用用户后不可再进行部分操作

#### 2.1.4 密码管理
- 支持修改用户密码
- 需校验旧密码
- 密码加密存储

#### 2.1.5 用户删除
- 支持逻辑删除（禁用）
- 记录删除原因

## 3. 数据模型

### 3.1 用户信息（UserState）

- 基本信息
  - 用户ID（id）
  - 用户名（username）
  - 姓名（name）
  - 账号ID（accountId）
  - 主邮箱（primaryEmail）
  - 主手机号（primaryPhone）
  - 头像（avatar）
- 状态信息
  - 状态（status：ACTIVE/DISABLED）
  - 部门编码（deptCode）
  - 上次登录时间（lastSignInAt）
- 安全信息
  - 加密密码（passwordEncrypted）
  - 加密方式（passwordEncryptionMethod）
- 档案信息
  - UserProfile（nickname, profile, website, gender, birthdate, locale, address）
- 其他信息
  - 自定义数据（customData: Map）
  - 身份信息（identities: Map）

### 3.2 用户档案（UserProfile）

- 昵称（nickname）
- 简介（profile）
- 个人网站（website）
- 性别（gender）
- 生日（birthdate）
- 地区（locale）
- 地址（address: Address）

### 3.3 地址（Address）

- 格式化地址（formatted）
- 街道（streetAddress）
- 城市（locality）
- 地区（region）
- 邮编（postalCode）
- 国家（country）

## 4. 命令模型

| 命令名                  | 字段                                                         | 说明                   |
|-------------------------|--------------------------------------------------------------|------------------------|
| CreateUser              | name, accountId, primaryEmail, primaryPhone, avatar, username| 创建新用户             |
| UpdateUser              | name, primaryEmail, primaryPhone, avatar                     | 更新基础信息           |
| DeleteUser              | reason                                                       | 禁用用户               |
| ChangeUserPassword      | oldPassword, newPassword                                     | 修改密码               |
| UpdateUserStatus        | status, reason                                               | 状态变更               |
| UpdateUserProfile       | nickname, profile, website, gender, birthdate, locale, address| 更新档案信息           |
| UpdateUserCustomData    | customData                                                   | 更新自定义数据         |

## 5. 事件模型

| 事件名                  | 字段                                                         | 说明                   |
|-------------------------|--------------------------------------------------------------|------------------------|
| UserCreated             | name, accountId, primaryEmail, primaryPhone, avatar, username| 用户已创建             |
| UserUpdated             | name, primaryEmail, primaryPhone, avatar                     | 基础信息已更新         |
| UserDeleted             | reason                                                       | 用户已禁用             |
| UserPasswordChanged     | encryptedPassword, encryptionMethod                          | 密码已变更             |
| UserStatusUpdated       | status, reason                                               | 状态已变更             |
| UserProfileUpdated      | nickname, profile, website, gender, birthdate, locale, address| 档案已变更             |
| UserCustomDataUpdated   | customData                                                   | 自定义数据已变更       |

## 6. 聚合关系图（PlantUML）

## 7. 接口设计（示例）

- POST /ums/user/create - 创建用户
- PUT /ums/user/update - 更新用户信息
- DELETE /ums/user/delete - 禁用用户
- PUT /ums/user/change-password - 修改密码
- PUT /ums/user/update-status - 更新状态
- PUT /ums/user/update-profile - 更新档案
- PUT /ums/user/update-custom-data - 更新自定义数据

## 8. 注意事项

1. 用户名、邮箱、手机号等需唯一性校验
2. 密码需加密存储，修改密码需校验旧密码
3. 禁用用户后不可再进行部分操作
4. 所有操作需记录操作日志
5. 用户状态、档案、自定义数据等变更需记录历史
6. 领域事件驱动状态变更，便于追溯 