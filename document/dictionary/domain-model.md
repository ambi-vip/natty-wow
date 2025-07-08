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
- 字典编码全局唯一（通过PrepareKey机制保证）
- 字典项编码在字典内唯一
- 只有ACTIVE状态的字典才能进行修改操作
- 字典项的默认值策略：itemValue为空时使用itemCode

### 2.2 值对象定义

#### DictionaryStatus - 字典状态
- `ACTIVE`: 启用状态（默认状态）
- `INACTIVE`: 禁用状态  
- `DELETED`: 删除状态

#### DictionaryItemStatus - 字典项状态  
- `ACTIVE`: 启用状态（默认状态）
- `INACTIVE`: 禁用状态

### 2.3 实体定义

#### DictionaryItem - 字典项实体
**属性**:
- itemCode: String - 字典项编码
- itemName: String - 字典项名称  
- itemValue: String - 字典项值
- sortOrder: Int - 排序号（默认0）
- description: String? - 描述信息
- localizedNames: Map<String, String>? - 多语言名称映射
- status: DictionaryItemStatus - 状态（默认ACTIVE）

## 3. 命令模型

### 3.1 字典命令
- `CreateDictionary` - 创建字典
  - code: String（必填，全局唯一）
  - name: String（必填）
  - description: String?
- `UpdateDictionary` - 更新字典信息
  - id: String（路径参数）
  - name: String（必填）
  - description: String?
- `ChangeDictionaryStatus` - 改变字典状态
  - id: String（路径参数）
  - status: DictionaryStatus

### 3.2 字典项命令
- `AddDictionaryItem` - 添加字典项
  - itemCode: String（必填）
  - itemName: String（必填）
  - itemValue: String?（为空时使用itemCode）
  - sortOrder: Int（默认0）
  - description: String?
  - localizedNames: Map<String, String>?
- `UpdateDictionaryItem` - 更新字典项
  - dictionaryId: String（路径参数）
  - itemCode: String（路径参数）
  - itemName: String（必填）
  - itemValue: String?
  - sortOrder: Int
  - description: String?
  - localizedNames: Map<String, String>?
- `ChangeDictionaryItemStatus` - 改变字典项状态
  - dictionaryId: String（路径参数）
  - itemCode: String（路径参数）
  - status: DictionaryItemStatus
- `RemoveDictionaryItem` - 移除字典项
  - dictionaryId: String（路径参数）
  - itemCode: String（路径参数）

## 4. 事件模型

### 4.1 字典事件
- `DictionaryCreated` - 字典已创建
  - dictionaryId, code, name, description
- `DictionaryUpdated` - 字典已更新
  - dictionaryId, name, description
- `DictionaryStatusChanged` - 字典状态已改变
  - dictionaryId, status
- `DictionaryDeleted` - 字典已删除
  - dictionaryId, code, name

### 4.2 字典项事件
- `DictionaryItemAdded` - 字典项已添加
  - dictionaryId, itemCode, itemName, itemValue, sortOrder, description, localizedNames
- `DictionaryItemUpdated` - 字典项已更新
  - dictionaryId, itemCode, itemName, itemValue, sortOrder, description, localizedNames
- `DictionaryItemStatusChanged` - 字典项状态已改变
  - dictionaryId, itemCode, status
- `DictionaryItemRemoved` - 字典项已移除
  - dictionaryId, itemCode

## 5. 业务规则

### 5.1 数据完整性约束
- 字典编码全局唯一（PrepareKey机制保证）
- 字典项编码在字典内唯一
- 只有ACTIVE状态的字典可进行修改操作
- 只有ACTIVE状态的字典项可进行更新操作
- 字典项必须关联到存在的字典

### 5.2 技术约束
- 基于事件溯源的状态管理
- CQRS读写分离
- 聚合版本号并发控制
- PrepareKey机制保证编码唯一性（创建时占用，删除时释放）
- 支持多语言和国际化（localizedNames）

### 5.3 状态转换规则
- 字典状态：ACTIVE ↔ INACTIVE ↔ DELETED
- 字典项状态：ACTIVE ↔ INACTIVE
- 状态变更需要验证当前状态与目标状态不同 