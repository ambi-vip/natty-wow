# 字典API规范文档

## 1. API概述

### 1.1 服务信息
- **服务名称**: common-service  
- **聚合名称**: dict
- **命令API基础路径**: `/api/common/dict`
- **查询API基础路径**: `/dict`
- **内容类型**: `application/json`

### 1.2 响应格式
```json
{
  "success": true,
  "code": "SUCCESS", 
  "message": "操作成功",
  "data": {},
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 2. 命令API端点详细说明

详细API接口参数和响应说明请参考Swagger文档：`/swagger-ui.html`

### 2.1 字典管理

#### 创建字典
- **端点**: `POST /api/common/dict`
- **描述**: 创建新字典
- **请求体**:
```json
{
  "code": "USER_STATUS",
  "name": "用户状态",
  "description": "用户状态字典"
}
```

#### 更新字典
- **端点**: `PUT /api/common/dict/{id}`
- **描述**: 更新字典基本信息
- **路径参数**: id - 字典ID
- **请求体**:
```json
{
  "name": "用户状态（更新）",
  "description": "更新后的描述"
}
```

#### 改变字典状态
- **端点**: `PUT /api/common/dict/{id}/status`
- **描述**: 改变字典状态
- **路径参数**: id - 字典ID
- **请求体**:
```json
{
  "status": "INACTIVE"
}
```

#### 删除字典
- **端点**: `DELETE /api/common/dict/{id}`
- **描述**: 删除字典（逻辑删除）
- **路径参数**: id - 字典ID

### 2.2 字典项管理

#### 添加字典项
- **端点**: `POST /api/common/dict/{id}/items`
- **描述**: 向字典添加字典项
- **路径参数**: id - 字典ID
- **请求体**:
```json
{
  "itemCode": "ACTIVE",
  "itemName": "激活",
  "itemValue": "1",
  "sortOrder": 1,
  "description": "用户激活状态",
  "localizedNames": {
    "en": "Active",
    "zh": "激活"
  }
}
```

#### 更新字典项
- **端点**: `PUT /api/common/dict/{id}/items/{itemCode}`
- **描述**: 更新字典项信息
- **路径参数**: 
  - id - 字典ID
  - itemCode - 字典项编码
- **请求体**:
```json
{
  "itemName": "激活状态",
  "itemValue": "1",
  "sortOrder": 1,
  "description": "更新后的描述"
}
```

#### 改变字典项状态
- **端点**: `PATCH /api/common/dict/{id}/items/{itemCode}/status`
- **描述**: 改变字典项状态
- **路径参数**: 
  - id - 字典ID
  - itemCode - 字典项编码
- **请求体**:
```json
{
  "status": "INACTIVE"
}
```

#### 移除字典项
- **端点**: `DELETE /api/common/dict/{id}/items/{itemCode}`
- **描述**: 从字典中移除字典项
- **路径参数**: 
  - id - 字典ID
  - itemCode - 字典项编码

## 3. 查询API端点详细说明

### 3.1 字典查询

#### 激活字典列表查询
- **端点**: `POST /dict/active/list`
- **描述**: 查询激活状态的字典列表，支持条件过滤、排序和限制
- **请求体**:
```json
{
  "condition": {
    "name": "用户"
  },
  "sort": [
    {
      "property": "name",
      "direction": "ASC"
    }
  ],
  "limit": 100
}
```
- **响应**:
```json
[
  {
    "id": "dict_001",
    "code": "USER_STATUS",
    "name": "用户状态",
    "items": [
      {
        "itemCode": "ACTIVE",
        "itemName": "激活",
        "itemValue": "1",
        "sortOrder": 1
      }
    ]
  }
]
```

#### 激活字典分页查询
- **端点**: `POST /dict/active/paged`
- **描述**: 分页查询激活状态的字典
- **请求体**:
```json
{
  "condition": {
    "name": "用户"
  },
  "sort": [
    {
      "property": "name",
      "direction": "ASC"
    }
  ],
  "pagination": {
    "index": 0,
    "size": 20
  }
}
```
- **响应**:
```json
{
  "total": 1,
  "list": [
    {
      "id": "dict_001",
      "code": "USER_STATUS", 
      "name": "用户状态",
      "items": [
        {
          "itemCode": "ACTIVE",
          "itemName": "激活",
          "itemValue": "1",
          "sortOrder": 1
        }
      ]
    }
  ]
}
```

#### 获取单个激活字典
- **端点**: `GET /dict/{dictionaryId}/active`
- **描述**: 根据字典ID获取单个激活状态的字典详情
- **路径参数**: dictionaryId - 字典ID
- **响应**:
```json
{
  "id": "dict_001",
  "code": "USER_STATUS",
  "name": "用户状态",
  "items": [
    {
      "itemCode": "ACTIVE",
      "itemName": "激活",
      "itemValue": "1",
      "sortOrder": 1
    }
  ]
}
```

## 4. 前端集成示例

### 4.1 TypeScript类型定义
```typescript
// 查询响应类型
interface DictionaryResponse {
  id: string;
  code: string;
  name: string;
  items: DictionaryItemResponse[];
}

interface DictionaryItemResponse {
  itemCode: string;
  itemName: string;
  itemValue: string;
  sortOrder: number;
}

// 查询请求类型
interface ListQueryRequest {
  condition?: Record<string, any>;
  sort?: Array<{
    property: string;
    direction: 'ASC' | 'DESC';
  }>;
  limit?: number;
}

interface PagedQueryRequest {
  condition?: Record<string, any>;
  sort?: Array<{
    property: string;
    direction: 'ASC' | 'DESC';
  }>;
  pagination: {
    index: number;
    size: number;
  };
}

interface PagedResponse<T> {
  total: number;
  list: T[];
}

// 命令API类型
interface Dictionary {
  id: string;
  code: string;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DELETED';
  items: DictionaryItem[];
}

interface DictionaryItem {
  itemCode: string;
  itemName: string;
  itemValue: string;
  sortOrder: number;
  status: 'ACTIVE' | 'INACTIVE';
  description?: string;
  localizedNames?: Record<string, string>;
}
```

### 4.2 字典服务封装
```typescript
class DictionaryService {
  private commandURL = '/api/common/dict';
  private queryURL = '/dict';
  
  // 查询API
  async listActiveDictionaries(request?: ListQueryRequest): Promise<DictionaryResponse[]> {
    const response = await fetch(`${this.queryURL}/active/list`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request || {})
    });
    return await response.json();
  }
  
  async pagedActiveDictionaries(request: PagedQueryRequest): Promise<PagedResponse<DictionaryResponse>> {
    const response = await fetch(`${this.queryURL}/active/paged`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    return await response.json();
  }
  
  async getActiveDictionary(dictionaryId: string): Promise<DictionaryResponse> {
    const response = await fetch(`${this.queryURL}/${dictionaryId}/active`);
    return await response.json();
  }
  
  // 命令API
  async createDictionary(dict: CreateDictionaryRequest): Promise<string> {
    const response = await fetch(`${this.commandURL}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dict)
    });
    const result = await response.json();
    return result.data;
  }
  
  async updateDictionary(id: string, dict: UpdateDictionaryRequest): Promise<void> {
    await fetch(`${this.commandURL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dict)
    });
  }
  
  async changeDictionaryStatus(id: string, status: string): Promise<void> {
    await fetch(`${this.commandURL}/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    });
  }
  
  async addDictionaryItem(dictionaryId: string, item: AddDictionaryItemRequest): Promise<void> {
    await fetch(`${this.commandURL}/${dictionaryId}/items`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(item)
    });
  }
}

interface CreateDictionaryRequest {
  code: string;
  name: string;
  description?: string;
}

interface UpdateDictionaryRequest {
  name: string;
  description?: string;
}

interface AddDictionaryItemRequest {
  itemCode: string;
  itemName: string;
  itemValue?: string;
  sortOrder?: number;
  description?: string;
  localizedNames?: Record<string, string>;
}
```

### 4.3 React组件示例
```tsx
import React, { useEffect, useState } from 'react';
import { Select, Table, Pagination } from 'antd';

// 字典选择组件
interface DictSelectProps {
  dictionaryCode: string;
  value?: string;
  onChange?: (value: string) => void;
}

export const DictSelect: React.FC<DictSelectProps> = ({ dictionaryCode, value, onChange }) => {
  const [options, setOptions] = useState<Array<{value: string, label: string}>>([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    const loadDictionary = async () => {
      setLoading(true);
      try {
        const dictService = new DictionaryService();
        const dictionaries = await dictService.listActiveDictionaries({
          condition: { code: dictionaryCode },
          limit: 1
        });
        
        if (dictionaries.length > 0) {
          const activeItems = dictionaries[0].items
            .sort((a, b) => a.sortOrder - b.sortOrder);
          
          setOptions(activeItems.map(item => ({
            value: item.itemValue,
            label: item.itemName
          })));
        }
      } catch (error) {
        console.error('加载字典失败:', error);
      } finally {
        setLoading(false);
      }
    };
    
    if (dictionaryCode) {
      loadDictionary();
    }
  }, [dictionaryCode]);
  
  return (
    <Select
      value={value}
      onChange={onChange}
      placeholder="请选择"
      loading={loading}
      options={options}
    />
  );
};
```

## 5. 错误处理

### 5.1 常见错误码
- `DICT_CODE_EXISTS` - 字典编码已存在
- `DICT_NOT_FOUND` - 字典不存在
- `DICT_ITEM_EXISTS` - 字典项编码已存在
- `DICT_ITEM_NOT_FOUND` - 字典项不存在
- `DICT_STATUS_INVALID` - 字典状态不允许操作
- `VALIDATION_ERROR` - 参数验证失败
