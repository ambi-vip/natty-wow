# 智能文件上传系统改造指南

## 概述

本次改造将原有的临时文件上传策略改为**无临时文件的二阶段智能上传架构**：

### 阶段一：智能上传策略
- **小文件直接上传**：文件大小小于阈值时，直接上传到最终存储位置
- **大文件预签名直传**：文件大小超过阈值时，生成预签名URL让客户端直接上传到云存储

### 阶段二：文件后处理
- 获取阶段一上传的文件，进行压缩、加密、缩略图生成等处理
- 支持异步处理，不阻塞上传流程

## 核心组件

### 1. SmartFileUploadController
智能上传控制器，提供以下接口：

```kotlin
// 获取上传策略
POST /api/storage/smart/upload/strategy
{
  "fileName": "example.pdf",
  "fileSize": 5242880,
  "bucketId": "bucket-123"
}

// 小文件直接上传
POST /api/storage/smart/upload/direct
Content-Type: multipart/form-data

// 大文件预签名上传
POST /api/storage/smart/upload/presigned
{
  "fileName": "large-video.mp4",
  "fileSize": 104857600,
  "bucketId": "bucket-123",
  "uploaderId": "user-456"
}

// 确认预签名上传完成
POST /api/storage/smart/upload/presigned/complete
{
  "fileId": "file-789",
  "actualFileSize": 104857600,
  "checksum": "sha256-hash"
}

// 触发文件处理
POST /api/storage/smart/process/{fileId}
{
  "enableCompression": true,
  "enableEncryption": false,
  "generateThumbnail": true
}
```

### 2. SmartUploadService
核心业务逻辑服务：

```kotlin
@Service
class SmartUploadService {
    // 确定上传策略（直接上传 vs 预签名上传）
    fun determineUploadStrategy(request: UploadStrategyRequest): Mono<UploadStrategyResponse>
    
    // 小文件直接上传
    fun directUpload(request: DirectUploadRequest): Mono<DirectUploadResponse>
    
    // 生成预签名上传URL
    fun generatePresignedUploadUrl(request: PresignedUploadRequest): Mono<PresignedUploadResponse>
    
    // 完成预签名上传
    fun completePresignedUpload(request: CompletePresignedUploadRequest): Mono<CompletePresignedUploadResponse>
    
    // 触发文件处理
    fun triggerFileProcessing(fileId: String, options: FileProcessingOptions): Mono<FileProcessingResponse>
}
```

### 3. FileProcessingService
文件后处理服务：

```kotlin
@Service
class FileProcessingService {
    // 启动异步文件处理
    fun startProcessing(fileId: String, storagePath: String, options: FileProcessingOptions): String
    
    // 查询处理状态
    fun getProcessingStatus(fileId: String): Mono<ProcessingStatusInfo>
}
```

### 4. 处理服务
- **CompressionService**：文件压缩服务（支持GZIP）
- **EncryptionService**：文件加密服务（支持AES-256）
- **EnhancedStorageStrategyService**：增强存储策略服务

## 配置

在 `application.yml` 中添加配置：

```yaml
natty:
  storage:
    smart-upload:
      # 默认直接上传阈值（10MB）
      default-direct-upload-threshold: 10485760
      # 预签名URL过期时间（1小时）
      presigned-url-expiration-seconds: 3600
      # 启用自动压缩
      enable-auto-compression: true
      # 启用自动加密
      enable-auto-encryption: false
      # 启用自动缩略图
      enable-auto-thumbnail: true
      # 处理超时时间（30分钟）
      processing-timeout-minutes: 30
      # 最大并发处理任务
      max-concurrent-processing-tasks: 10
```

## 使用流程

### 小文件上传流程

1. **获取上传策略**
```bash
curl -X POST /api/storage/smart/upload/strategy \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "document.pdf",
    "fileSize": 2048576,
    "bucketId": "my-bucket"
  }'
```

2. **直接上传**（如果策略返回 DIRECT_UPLOAD）
```bash
curl -X POST /api/storage/smart/upload/direct \
  -F "file=@document.pdf" \
  -F "bucketId=my-bucket" \
  -F "uploaderId=user-123" \
  -F "enableCompression=true"
```

### 大文件上传流程

1. **获取预签名URL**
```bash
curl -X POST /api/storage/smart/upload/presigned \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "video.mp4",
    "fileSize": 104857600,
    "bucketId": "my-bucket",
    "uploaderId": "user-123"
  }'
```

2. **直接上传到云存储**（使用返回的预签名URL）
```bash
curl -X PUT "https://presigned-upload-url" \
  --data-binary @video.mp4 \
  -H "Content-Type: video/mp4"
```

3. **确认上传完成**
```bash
curl -X POST /api/storage/smart/upload/presigned/complete \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "file-456",
    "actualFileSize": 104857600,
    "checksum": "sha256-hash"
  }'
```

### 文件处理流程

1. **触发处理**
```bash
curl -X POST /api/storage/smart/process/file-456 \
  -H "Content-Type: application/json" \
  -d '{
    "enableCompression": true,
    "enableEncryption": true,
    "generateThumbnail": false
  }'
```

2. **查询处理状态**
```bash
curl -X GET /api/storage/smart/process/file-456/status
```

## 优势

### 相比原有临时文件策略的优势：

1. **性能提升**
   - 消除临时文件I/O开销
   - 大文件直传云存储，减少服务器带宽压力
   - 异步处理不阻塞上传流程

2. **存储优化**
   - 无需临时存储空间
   - 减少磁盘I/O操作
   - 自动清理机制简化

3. **扩展性**
   - 支持多种云存储后端
   - 灵活的处理策略配置
   - 易于添加新的处理类型

4. **用户体验**
   - 上传速度更快
   - 支持大文件上传
   - 实时处理状态反馈

## 迁移指南

### 从旧系统迁移：

1. **保留现有接口**：原有的上传接口可以继续使用，逐步迁移到新接口
2. **配置调整**：移除临时文件相关配置，添加智能上传配置
3. **数据迁移**：现有文件无需迁移，新上传使用新策略
4. **监控调整**：更新监控指标，关注处理队列状态

### 注意事项：

1. **向后兼容**：确保现有客户端能正常工作
2. **错误处理**：完善异常处理和重试机制
3. **安全考虑**：预签名URL的安全性和过期时间
4. **监控告警**：处理队列积压和失败率监控

## 扩展点

系统设计了多个扩展点，便于后续功能增强：

1. **存储策略扩展**：支持新的云存储提供商
2. **处理器扩展**：添加新的文件处理类型（如转码、OCR等）
3. **策略扩展**：支持更复杂的上传策略决策
4. **监控扩展**：集成更多监控和指标收集

这个新架构为文件管理系统提供了更好的性能、扩展性和用户体验。