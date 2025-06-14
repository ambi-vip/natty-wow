# 回款管理模块需求文档

## 1. 功能概述

回款管理模块是 CRM 系统的核心业务模块，用于管理合同回款和回款计划。该模块支持回款的创建、审批、执行等环节的管理，并与合同、客户等模块紧密关联。

## 2. 功能需求

### 2.1 基础功能

#### 2.1.1 回款创建
- 支持手动创建新回款
- 必填信息包括：
  - 回款编号
  - 所属客户
  - 关联合同
  - 回款金额
  - 负责人
  - 回款时间
- 可选信息包括：
  - 回款方式
  - 回款备注
  - 回款状态

#### 2.1.2 回款查询
- 支持分页查询回款列表
- 支持多条件筛选：
  - 回款编号
  - 所属客户
  - 关联合同
  - 负责人
  - 回款时间
  - 回款状态
  - 回款金额
- 支持导出 Excel
- 支持按客户查询回款
- 支持按合同查询回款

#### 2.1.3 回款详情
- 显示回款的完整信息
- 包含：
  - 基本信息
  - 所属客户信息
  - 关联合同信息
  - 负责人信息
  - 回款计划
  - 操作日志

#### 2.1.4 回款编辑
- 支持修改回款信息
- 记录修改历史
- 权限控制

#### 2.1.5 回款删除
- 支持删除回款
- 删除前进行确认
- 记录删除操作

### 2.2 高级功能

#### 2.2.1 回款审批
- 支持回款提交审批
- 支持回款审批流程
- 记录审批历史
- 支持审批通知

#### 2.2.2 回款计划
- 支持创建回款计划
- 支持修改回款计划
- 支持删除回款计划
- 支持查询回款计划
- 支持回款计划提醒

#### 2.2.3 回款统计
- 支持回款金额统计
- 支持回款进度统计
- 支持回款逾期统计
- 支持回款趋势分析

#### 2.2.4 回款提醒
- 支持回款到期提醒
- 支持回款逾期提醒
- 支持回款计划提醒
- 支持提醒配置

### 2.3 权限控制

- 创建权限：crm:receivable:create
- 更新权限：crm:receivable:update
- 删除权限：crm:receivable:delete
- 查询权限：crm:receivable:query
- 导出权限：crm:receivable:export
- 计划管理权限：crm:receivable-plan:*

## 3. 数据模型

### 3.1 回款信息（CrmReceivableDO）
- 基本信息
  - 回款ID
  - 回款编号
  - 客户ID
  - 合同ID
  - 回款金额
  - 回款时间
- 负责人信息
  - 负责人ID
  - 负责人部门
- 时间信息
  - 创建时间
  - 更新时间
- 状态信息
  - 回款状态
  - 审批状态
- 其他信息
  - 回款方式
  - 备注

### 3.2 回款计划（CrmReceivablePlanDO）
- 计划信息
  - 计划ID
  - 回款ID
  - 客户ID
  - 合同ID
  - 计划金额
  - 计划时间
  - 回款期数
  - 回款类型
- 负责人信息
  - 负责人ID
- 时间信息
  - 创建时间
  - 更新时间
- 状态信息
  - 计划状态
- 其他信息
  - 备注

## 4. 接口设计

### 4.1 回款管理接口
- POST /crm/receivable/create - 创建回款
- PUT /crm/receivable/update - 更新回款
- DELETE /crm/receivable/delete - 删除回款
- GET /crm/receivable/get - 获取回款详情
- GET /crm/receivable/page - 获取回款分页列表
- GET /crm/receivable/export-excel - 导出回款Excel

### 4.2 回款查询接口
- GET /crm/receivable/page-by-customer - 获取客户下的回款列表

### 4.3 回款审批接口
- PUT /crm/receivable/submit - 提交回款审批

### 4.4 回款计划接口
- POST /crm/receivable-plan/create - 创建回款计划
- PUT /crm/receivable-plan/update - 更新回款计划
- DELETE /crm/receivable-plan/delete - 删除回款计划
- GET /crm/receivable-plan/get - 获取回款计划详情
- GET /crm/receivable-plan/page - 获取回款计划分页列表
- GET /crm/receivable-plan/export-excel - 导出回款计划Excel
- GET /crm/receivable-plan/simple-list - 获取回款计划精简列表

### 4.5 回款统计接口
- GET /crm/receivable/audit-count - 获取待审核回款数量
- GET /crm/receivable-plan/remind-count - 获取待回款提醒数量

## 5. 注意事项

1. 回款必须关联客户和合同
2. 回款审批流程需要符合业务规则
3. 所有操作需要记录操作日志
4. 导出数据需要考虑性能问题
5. 回款状态变更需要记录变更历史
6. 回款金额计算需要考虑精度问题
7. 回款计划需要支持多期回款
8. 回款提醒需要支持自定义配置
9. 回款统计需要支持多维度分析
10. 回款计划需要支持自动提醒 