# 字典API规范文档

## 1. API概述

### 1.1 服务信息
- **服务名称**: common-service  
- **聚合名称**: dict
- **基础路径**: `/api/common/dict`
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

## 2. 主要API端点

详细API接口说明请参考Swagger文档：`/swagger-ui.html`

### 2.1 字典管理
- `POST /api/common/dict` - 创建字典
- `PUT /api/common/dict/{id}` - 更新字典
- `GET /api/common/dict/{id}` - 查询字典详情
- `GET /api/common/dict` - 字典列表查询
- `PUT /api/common/dict/{id}/status` - 改变字典状态

### 2.2 字典项管理
- `POST /api/common/dict/{id}/items` - 添加字典项
- `PUT /api/common/dict/{id}/items/{itemCode}` - 更新字典项
- `DELETE /api/common/dict/{id}/items/{itemCode}` - 删除字典项
- `PUT /api/common/dict/{id}/items/{itemCode}/status` - 改变字典项状态
- `GET /api/common/dict/{id}/items` - 字典项列表查询

### 2.3 查询优化
- `GET /api/common/dict/code/{code}` - 根据编码查询字典
- `POST /api/common/dict/batch` - 批量查询字典
- `GET /api/common/dict/{id}/items/active` - 活跃字典项查询

## 3. 前端集成示例

### 3.1 TypeScript类型定义
```typescript
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

### 3.2 字典服务封装
```typescript
class DictionaryService {
  private baseURL = '/api/common/dict';
  
  async getDictionaryByCode(code: string): Promise<Dictionary> {
    const response = await fetch(`${this.baseURL}/code/${code}`);
    const result = await response.json();
    return result.data;
  }
  
  async getActiveItems(dictionaryId: string): Promise<DictionaryItem[]> {
    const response = await fetch(`${this.baseURL}/${dictionaryId}/items/active`);
    const result = await response.json();
    return result.data;
  }
}
```

### 3.3 React字典组件
```tsx
import React, { useEffect, useState } from 'react';
import { Select } from 'antd';

interface DictSelectProps {
  dictCode: string;
  value?: string;
  onChange?: (value: string) => void;
}

export const DictSelect: React.FC<DictSelectProps> = ({ dictCode, value, onChange }) => {
  const [options, setOptions] = useState<Array<{value: string, label: string}>>([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    const loadDictionary = async () => {
      setLoading(true);
      try {
        const dictService = new DictionaryService();
        const dictionary = await dictService.getDictionaryByCode(dictCode);
        const activeItems = dictionary.items
          .filter(item => item.status === 'ACTIVE')
          .sort((a, b) => a.sortOrder - b.sortOrder);
        
        setOptions(activeItems.map(item => ({
          value: item.itemValue,
          label: item.itemName
        })));
      } catch (error) {
        console.error('加载字典失败:', error);
      } finally {
        setLoading(false);
      }
    };
    
    if (dictCode) {
      loadDictionary();
    }
  }, [dictCode]);
  
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