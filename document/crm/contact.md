# 联系人管理模块需求文档

## 1. 功能概述

联系人管理模块是 CRM 系统的重要组成部分，用于管理客户相关的联系人信息。该模块支持联系人的创建、管理、分配以及与客户、商机的关联管理。

## 2. 功能需求

### 2.1 基础功能

#### 2.1.1 联系人创建
- 支持手动创建新联系人
- 必填信息包括：
  - 联系人姓名
  - 所属客户
  - 联系方式
  - 负责人
- 可选信息包括：
  - 职位
  - 部门
  - 地区
  - 直属上级
  - 备注信息

#### 2.1.2 联系人查询
- 支持分页查询联系人列表
- 支持多条件筛选：
  - 联系人姓名
  - 所属客户
  - 负责人
  - 创建时间
  - 地区
- 支持导出 Excel
- 支持按客户查询联系人
- 支持按商机查询联系人

#### 2.1.3 联系人详情
- 显示联系人的完整信息
- 包含：
  - 基本信息
  - 所属客户信息
  - 直属上级信息
  - 负责人信息
  - 相关商机
  - 操作日志

#### 2.1.4 联系人编辑
- 支持修改联系人信息
- 记录修改历史
- 权限控制

#### 2.1.5 联系人删除
- 支持删除联系人
- 删除前进行确认
- 记录删除操作

### 2.2 高级功能

#### 2.2.1 联系人分配
- 支持联系人转移给其他负责人
- 记录转移历史
- 支持分配通知

#### 2.2.2 商机关联
- 支持联系人与商机关联
- 支持批量关联商机
- 支持解除商机关联
- 记录关联历史

### 2.3 权限控制

- 创建权限：crm:contact:create
- 更新权限：crm:contact:update
- 删除权限：crm:contact:delete
- 查询权限：crm:contact:query
- 导出权限：crm:contact:export
- 商机关联权限：crm:contact:create-business
- 商机解除权限：crm:contact:delete-business

## 3. 数据模型

### 3.1 联系人信息（CrmContactDO）
- 基本信息
  - 联系人ID
  - 联系人姓名
  - 客户ID
  - 地区ID
  - 职位
  - 部门
- 联系方式
  - 手机号
  - 邮箱
  - 电话
- 负责人信息
  - 负责人ID
  - 负责人部门
- 时间信息
  - 创建时间
  - 更新时间
- 其他信息
  - 直属上级ID
  - 备注

### 3.2 商机关联（CrmContactBusinessDO）
- 关联信息
  - 联系人ID
  - 商机ID
  - 创建时间

## 4. 接口设计

### 4.1 联系人管理接口
- POST /crm/contact/create - 创建联系人
- PUT /crm/contact/update - 更新联系人
- DELETE /crm/contact/delete - 删除联系人
- GET /crm/contact/get - 获取联系人详情
- GET /crm/contact/page - 获取联系人分页列表
- GET /crm/contact/export-excel - 导出联系人Excel
- GET /crm/contact/simple-all-list - 获取联系人精简列表

### 4.2 联系人查询接口
- GET /crm/contact/page-by-customer - 获取客户下的联系人列表
- GET /crm/contact/page-by-business - 获取商机下的联系人列表

### 4.3 联系人转移接口
- PUT /crm/contact/transfer - 联系人转移

### 4.4 商机关联接口
- POST /crm/contact/create-business-list - 创建商机关联
- POST /crm/contact/create-business-list2 - 创建商机关联（批量）
- DELETE /crm/contact/delete-business-list - 删除商机关联
- DELETE /crm/contact/delete-business-list2 - 删除商机关联（批量）

## 5. 注意事项

1. 联系人必须关联客户
2. 联系人分配需要考虑数据权限
3. 所有操作需要记录操作日志
4. 导出数据需要考虑性能问题
5. 联系人状态变更需要记录变更历史
6. 商机关联需要考虑数据一致性
7. 联系人转移需要考虑关联数据的处理 