# 线索管理模块需求文档

## 1. 功能概述

线索管理模块是 CRM 系统的入口模块，用于管理潜在客户的信息和跟进状态。该模块主要负责收集、管理和转化销售线索。

## 2. 功能需求

### 2.1 基础功能

#### 2.1.1 线索创建
- 支持手动创建新线索
- 必填信息包括：
  - 线索名称
  - 客户信息
  - 联系人信息
  - 地区信息
  - 负责人
- 可选信息包括：
  - 线索来源
  - 线索状态
  - 备注信息

#### 2.1.2 线索查询
- 支持分页查询线索列表
- 支持多条件筛选：
  - 线索名称
  - 客户名称
  - 负责人
  - 创建时间
  - 地区
  - 状态
- 支持导出 Excel

#### 2.1.3 线索详情
- 显示线索的完整信息
- 包含：
  - 基本信息
  - 客户信息
  - 联系人信息
  - 跟进记录
  - 操作日志

#### 2.1.4 线索编辑
- 支持修改线索信息
- 记录修改历史
- 权限控制

#### 2.1.5 线索删除
- 支持删除线索
- 删除前进行确认
- 记录删除操作

### 2.2 高级功能

#### 2.2.1 线索转化
- 支持将线索转化为客户
- 转化时自动创建客户信息
- 保留原始线索信息
- 记录转化历史

#### 2.2.2 线索分配
- 支持线索转移给其他负责人
- 转移时可以选择是否通知新负责人
- 记录转移历史

#### 2.2.3 线索跟进
- 显示待跟进的线索数量
- 支持设置跟进提醒
- 记录跟进历史

### 2.3 权限控制

- 创建权限：crm:clue:create
- 更新权限：crm:clue:update
- 删除权限：crm:clue:delete
- 查询权限：crm:clue:query
- 导出权限：crm:clue:export

## 3. 数据模型

### 3.1 线索信息（ClueState）

- 基本信息
  - 线索ID (`id`): 编号，主键
  - 线索名称 (`name`)
  - 客户ID (`customerId`)
  - 转化状态 (`transformStatus`): true 表示已转换，会更新客户编号
  - 所在地 (`areaId`): 关联地区 ID
  - 所属行业 (`industryId`): 对应字典 CRM_CUSTOMER_INDUSTRY
  - 客户等级 (`level`): 对应字典 CRM_CUSTOMER_LEVEL
  - 客户来源 (`source`): 对应字典 CRM_CUSTOMER_SOURCE
  - 备注 (`remark`)
- 负责人信息
  - 负责人用户编号 (`ownerUserId`)
- 跟进信息
  - 跟进状态 (`followUpStatus`)
  - 最后跟进时间 (`contactLastTime`)
  - 最后跟进内容 (`contactLastContent`)
  - 下次联系时间 (`contactNextTime`)
- 联系方式信息
  - 手机号 (`mobile`)
  - 电话 (`telephone`)
  - QQ (`qq`)
  - 微信 (`wechat`)
  - 邮箱 (`email`)
  - 详细地址 (`detailAddress`)
- 时间信息
  - 创建时间 (`createTime`)
  - 更新时间 (`updateTime`)

#### 字段说明

- `id`：唯一标识线索的主键。
- `name`：线索名称。
- `customerId`：转化后关联的客户ID。
- `transformStatus`：线索是否已转化为客户。
- `areaId`：地区ID，关联地区表。
- `industryId`：所属行业，字典值。
- `level`：客户等级，字典值。
- `source`：客户来源，字典值。
- `remark`：备注信息。
- `ownerUserId`：负责人用户编号。
- `followUpStatus`：是否有跟进记录。
- `contactLastTime`：最后一次跟进时间。
- `contactLastContent`：最后一次跟进内容。
- `contactNextTime`：下次计划联系时间。
- `mobile`、`telephone`、`qq`、`wechat`、`email`、`detailAddress`：联系方式相关字段。
- `createTime`：创建时间。
- `updateTime`：最后更新时间。

### 3.1.1 线索信息数据结构（ClueState.kt 分析）

| 字段名              | 类型                    | 可变性   | 说明                         |
|---------------------|-------------------------|----------|------------------------------|
| id                  | String                  | val      | 线索ID，主键                 |
| name                | String?                 | var      | 线索名称                     |
| ownerUserId         | String?                 | var      | 负责人用户编号               |
| contactInfo         | ContactInfo?            | var      | 联系方式对象                 |
| industryId          | String?                 | var      | 所属行业                     |
| level               | String?                 | var      | 客户等级                     |
| source              | String?                 | var      | 客户来源                     |
| remark              | String?                 | var      | 备注                         |
| status              | ClueStatus              | var      | 线索状态                     |
| transformStatus     | Boolean                 | var      | 转化状态                     |
| customerId          | String?                 | var      | 客户ID（转化后）             |
| followUpStatus      | Boolean                 | var      | 跟进状态                     |
| contactLastTime     | LocalDateTime?          | var      | 最后跟进时间                 |
| contactLastContent  | String?                 | var      | 最后跟进内容                 |
| contactNextTime     | LocalDateTime?          | var      | 下次联系时间                 |

#### 说明：
- `ContactInfo` 为复合类型，包含手机号、电话、QQ、微信、邮箱、详细地址等字段。
- `ClueStatus` 为枚举类型，包含：NEW（新建）、FOLLOWING（跟进中）、QUALIFIED（已转化）、INVALID（无效）。
- 字段均为私有 set，仅允许领域事件变更。
- 通过事件溯源（@OnSourcing）进行状态变更。

### 3.1.2 联系方式信息数据结构（ContactInfo.kt 分析）

| 字段名       | 类型      | 可变性 | 说明       |
|------------|-----------|------|------------|
| customerName | String?   | val    | 客户名称     |
| customerId | String?   | val    | 客户ID      |
| mobile     | String?   | val    | 手机号      |
| telephone  | String?   | val    | 电话       |
| qq         | String?   | val    | QQ 号码    |
| wechat     | String?   | val    | 微信号      |
| email      | String?   | val    | 邮箱       |
| areaId     | String?   | var    | 地区ID     |
| address    | String?   | var    | 详细地址     |

#### 说明：
- `ContactInfo` 是一个值对象（ValueObject），用于封装联系方式信息。
- 字段均为可空类型，表示可以为空。
- `customerName` 和 `customerId` 字段在 `ContactInfo` 中可能用于记录线索转化后对应的客户信息。
- `areaId` 和 `address` 是可变字段，允许后续修改。

## 4. 接口设计

### 4.1 线索管理接口
- POST /crm/clue/create - 创建线索
- PUT /crm/clue/update - 更新线索
- DELETE /crm/clue/delete - 删除线索
- GET /crm/clue/get - 获取线索详情
- GET /crm/clue/page - 获取线索分页列表
- GET /crm/clue/export-excel - 导出线索Excel

### 4.2 线索转化接口
- PUT /crm/clue/transfer - 线索转移
- PUT /crm/clue/transform - 线索转化为客户
- GET /crm/clue/follow-count - 获取待跟进线索数量

## 5. 注意事项

1. 线索转化后需要保留历史记录
2. 线索分配需要考虑数据权限
3. 所有操作需要记录操作日志
4. 导出数据需要考虑性能问题
5. 线索状态变更需要记录变更历史 