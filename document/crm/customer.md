# 客户管理模块需求文档

## 1. 功能概述

客户管理模块是 CRM 系统的核心模块，用于管理客户信息、客户跟进、客户分配等核心业务功能。该模块支持客户全生命周期的管理，包括客户创建、跟进、转化、分配等环节。

## 2. 功能需求

### 2.1 基础功能

#### 2.1.1 客户创建
- 支持手动创建新客户
- 支持从线索转化创建客户
- 必填信息包括：
  - 客户名称
  - 客户类型
  - 地区信息
  - 负责人
- 可选信息包括：
  - 客户来源
  - 客户状态
  - 备注信息

#### 2.1.2 客户查询
- 支持分页查询客户列表
- 支持多条件筛选：
  - 客户名称
  - 客户类型
  - 负责人
  - 创建时间
  - 地区
  - 状态
- 支持导出 Excel
- 支持导入客户数据

#### 2.1.3 客户详情
- 显示客户的完整信息
- 包含：
  - 基本信息
  - 联系人信息
  - 跟进记录
  - 操作日志
  - 相关商机
  - 相关合同

#### 2.1.4 客户编辑
- 支持修改客户信息
- 记录修改历史
- 权限控制

#### 2.1.5 客户删除
- 支持删除客户
- 删除前进行确认
- 记录删除操作

### 2.2 高级功能

#### 2.2.1 客户公海池
- 支持客户放入公海池
- 支持从公海池领取客户
- 支持公海池客户分配
- 自动计算客户进入公海时间
- 支持公海池配置管理

#### 2.2.2 客户分配
- 支持客户转移给其他负责人
- 支持批量分配客户
- 记录分配历史
- 支持分配通知

#### 2.2.3 客户锁定
- 支持锁定/解锁客户
- 锁定客户不可被其他用户操作
- 记录锁定历史

#### 2.2.4 客户跟进
- 显示待跟进的客户数量
- 显示今日需联系客户数量
- 支持设置跟进提醒
- 记录跟进历史

#### 2.2.5 成交状态管理
- 支持更新客户成交状态
- 记录成交状态变更历史
- 影响客户公海池规则

### 2.3 权限控制

- 创建权限：crm:customer:create
- 更新权限：crm:customer:update
- 删除权限：crm:customer:delete
- 查询权限：crm:customer:query
- 导出权限：crm:customer:export
- 导入权限：crm:customer:import
- 领取权限：crm:customer:receive
- 分配权限：crm:customer:distribute

## 3. 数据模型

### 3.1 客户信息（CrmCustomerDO）
- 基本信息
  - 客户ID
  - 客户名称
  - 客户类型
  - 地区ID
  - 状态
  - 来源
- 负责人信息
  - 负责人ID
  - 负责人部门
- 时间信息
  - 创建时间
  - 更新时间
  - 负责人时间
  - 最后联系时间
- 状态信息
  - 成交状态
  - 锁定状态
- 其他信息
  - 备注

### 3.2 公海池配置（CrmCustomerPoolConfigDO）
- 配置信息
  - 是否启用
  - 未成交放入公海天数
  - 未跟进放入公海天数

## 4. 接口设计

### 4.1 客户管理接口
- POST /crm/customer/create - 创建客户
- PUT /crm/customer/update - 更新客户
- DELETE /crm/customer/delete - 删除客户
- GET /crm/customer/get - 获取客户详情
- GET /crm/customer/page - 获取客户分页列表
- GET /crm/customer/export-excel - 导出客户Excel
- POST /crm/customer/import - 导入客户
- GET /crm/customer/get-import-template - 获取导入模板

### 4.2 客户公海池接口
- PUT /crm/customer/put-pool - 放入公海
- PUT /crm/customer/receive - 领取公海客户
- PUT /crm/customer/distribute - 分配公海客户
- GET /crm/customer/put-pool-remind-page - 获取待进入公海客户
- GET /crm/customer/put-pool-remind-count - 获取待进入公海客户数量

### 4.3 客户状态接口
- PUT /crm/customer/update-deal-status - 更新成交状态
- PUT /crm/customer/lock - 锁定/解锁客户
- PUT /crm/customer/transfer - 转移客户

### 4.4 客户统计接口
- GET /crm/customer/today-contact-count - 获取今日需联系客户数量
- GET /crm/customer/follow-count - 获取待跟进客户数量
- GET /crm/customer/simple-list - 获取客户精简列表

## 5. 注意事项

1. 客户公海池规则需要可配置
2. 客户分配需要考虑数据权限
3. 所有操作需要记录操作日志
4. 导入导出需要考虑性能问题
5. 客户状态变更需要记录变更历史
6. 客户锁定机制需要防止死锁
7. 公海池客户领取需要考虑并发问题 