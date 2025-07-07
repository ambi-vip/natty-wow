# 字典领域模型文档

## 1. 领域概述

字典领域负责管理系统中的基础数据字典，提供键值对映射关系的存储和查询功能。采用事件溯源(Event Sourcing)和CQRS模式，为其他业务领域提供标准化的数据引用服务。

**通用语言**:
- **字典(Dictionary)**: 具有唯一编码的数据分类容器
- **字典项(DictionaryItem)**: 字典内的具体键值对数据
- **字典编码(Code)**: 字典的全局唯一标识符
- **字典项编码(ItemCode)**: 字典项在字典内的唯一标识符

## 2. 聚合设计

### 2.1 聚合根 - Dictionary
**职责**: 管理字典及字典项的完整生命周期，保证聚合内数据一致性

**聚合边界**:
```
Dictionary Aggregate
├── Dictionary (聚合根)
├── DictionaryItem (实体)
├── DictionaryStatus (值对象)
└── DictionaryItemStatus (值对象)
```

**核心约束**:
- 字典编码全局唯一
- 字典项编码在字典内唯一
- 只有ACTIVE状态的字典才能进行修改操作

### 2.2 值对象定义

#### DictionaryStatus - 字典状态
- `ACTIVE`: 启用状态
- `INACTIVE`: 禁用状态  
- `DELETED`: 删除状态

#### DictionaryItemStatus - 字典项状态  
- `ACTIVE`: 启用状态
- `INACTIVE`: 禁用状态

## 3. 命令模型

### 3.1 字典命令
- `CreateDictionary` - 创建字典
- `UpdateDictionary` - 更新字典信息
- `ChangeDictionaryStatus` - 改变字典状态

### 3.2 字典项命令
- `AddDictionaryItem` - 添加字典项
- `UpdateDictionaryItem` - 更新字典项
- `ChangeDictionaryItemStatus` - 改变字典项状态
- `RemoveDictionaryItem` - 移除字典项

## 4. 事件模型

### 4.1 字典事件
- `DictionaryCreated` - 字典已创建
- `DictionaryUpdated` - 字典已更新
- `DictionaryStatusChanged` - 字典状态已改变
- `DictionaryDeleted` - 字典已删除

### 4.2 字典项事件
- `DictionaryItemAdded` - 字典项已添加
- `DictionaryItemUpdated` - 字典项已更新
- `DictionaryItemStatusChanged` - 字典项状态已改变
- `DictionaryItemRemoved` - 字典项已移除

## 5. 业务规则

### 5.1 数据完整性约束
- 字典编码全局唯一
- 字典项编码在字典内唯一
- 只有ACTIVE状态的字典可进行修改操作
- 字典项必须关联到存在的字典

### 5.2 技术约束
- 基于事件溯源的状态管理
- CQRS读写分离
- 聚合版本号并发控制 