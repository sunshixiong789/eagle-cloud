# 文件存储规范

技术栈：`eagle-oss-minio-starter`（MinIO 8.5.17）。

## 何时存 OSS、何时存 DB

| 数据                  | 位置         |
|---------------------|------------|
| 用户上传文件（图片/视频/附件）    | OSS        |
| 临时大对象（导出 Excel、PDF） | OSS（短 TTL） |
| 头像、Logo、产品图         | OSS        |
| 富文本中嵌入的图片           | OSS        |
| 业务消息体（短文本）          | DB         |
| 配置 / 字典             | DB         |

**禁止**把图片 / 文件 base64 存进 DB（性能灾难）。

## Bucket 设计

按业务分 Bucket，不按文件类型：

```
eagle-prod-user-avatar      # 用户头像
eagle-prod-product-images   # 产品图
eagle-prod-order-attach     # 订单附件
eagle-prod-export           # 导出文件（24h TTL）
eagle-prod-public           # 公开资源
eagle-prod-tenant-{id}      # 租户专属（按需）
```

- 命名 `eagle-{env}-{purpose}`，全小写、kebab-case
- 默认**私有**（`private` ACL），通过签名 URL 访问
- 仅 `public` Bucket 公开读

## 对象 Key 设计

格式：`{tenant}/{biz-type}/{yyyy/MM/dd}/{uuid}.{ext}`

```
t001/order/2026/04/30/8f3a1b2c-9d7e-4f5g-h6i7-j8k9l0m1n2o3.pdf
t001/avatar/2026/04/30/u1024-7b3e9f12.jpg
```

- 多租户必须以 `tenantId` 开头（隔离）
- 日期分层避免单目录文件过多（影响 list 性能）
- 文件名**强制** UUID（防文件名注入 / 防猜测 / 防碰撞）
- 保留原始扩展名（白名单内）

**禁止**直接用上传文件名作为 Key（XSS / 路径穿越 / 隐私泄漏）。

## 上传校验

```java
// ✅ 必须校验
public UploadResponse upload(MultipartFile file) {
    validateSize(file, MAX_SIZE);              // 文件大小
    validateExtension(file, ALLOWED_EXT);      // 后缀白名单
    validateMimeType(file, ALLOWED_MIME);      // 真实 MIME（魔数检测）
    validateFileName(file.getOriginalFilename()); // 防路径穿越（拒绝 `../`）
    String key = buildKey(file);
    return ossService.upload(bucket, key, file.getInputStream());
}
```

| 校验项      | 默认值                                                   |
|----------|-------------------------------------------------------|
| 大小       | 图片 ≤ 5MB；附件 ≤ 50MB；视频 ≤ 500MB                         |
| 后缀白名单    | 图片：`.jpg .jpeg .png .gif .webp`；文档：`.pdf .docx .xlsx` |
| MIME 真实性 | 用 Apache Tika 检测魔数，**不**信任前端传的 `Content-Type`         |
| 文件名      | 拒绝 `..` / `/` / 空字符 / 控制字符                            |

**禁止**：可执行文件（`.exe .sh .bat .jsp .php`）即使白名单也不接受。

## 病毒扫描

生产环境集成 ClamAV 异步扫描：

```java
// 上传后异步触发
publisher.publish("prod_oss_uploaded",new FileUploadedEvent(bucket, key));

// 扫描消费者
@Override
public void handle(FileUploadedEvent event) {
    if (clamAv.scan(bucket, key).isInfected()) {
        ossService.remove(bucket, key);
        alarm.notify("infected file: " + key);
    }
}
```

## 访问控制

### 私有文件（默认）

```java
// ✅ 签名 URL，TTL 短（≤ 1 小时）
String url = ossService.presignedGetUrl(bucket, key, Duration.ofMinutes(15));
```

- 签名 URL **必须**含 TTL，**禁止**永久 URL
- 越权检查：生成 URL 前校验当前用户是否有权访问该资源
- 多租户场景：`bucket / key` 必须包含目标 `tenantId`，且与当前租户匹配

### 公开文件

```
public-read 仅用于：CDN 加速的静态资源（产品图、Logo）
```

直接 URL 访问；**禁止**包含敏感信息或可推测路径。

## 上传方式

| 场景            | 方式                             |
|---------------|--------------------------------|
| 小文件（< 5MB）    | 直接 multipart 上传到后端 → 后端转存 OSS  |
| 大文件（> 5MB）    | **直传 OSS**：后端签名 URL，浏览器 PUT 直传 |
| 超大文件（> 100MB） | 分片上传（MinIO `Multipart Upload`） |

```java
// ✅ 直传签名（推荐大文件）
String uploadUrl = ossService.presignedPutUrl(bucket, key, Duration.ofMinutes(10));
return new

UploadTicket(uploadUrl, key);
```

**禁止**：用户文件经过应用服务器转发（带宽 / 内存 / CPU 浪费）。

## 临时文件清理

```java
// ✅ 导出场景：写入临时 Bucket，TTL 自动清理
@Bean
public LifecycleConfiguration exportBucketLifecycle() {
    return LifecycleConfiguration.builder()
            .addRule(rule -> rule.id("auto-expire-7d").expiration(7))
            .build();
}
```

或定时任务清理超过 N 天未引用的对象。

**禁止**临时文件长期堆积——单 Bucket 文件数 > 1000 万会影响管理操作。

## 文件元数据

业务表存储文件元数据：

```sql
CREATE TABLE t_file
(
    id            BIGINT PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    bucket        VARCHAR(64)  NOT NULL,
    object_key    VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    size          BIGINT       NOT NULL,
    content_type  VARCHAR(128),
    md5           CHAR(32),
    uploaded_by   VARCHAR(64),
    uploaded_at   TIMESTAMP,
    deleted       TINYINT(1)   DEFAULT 0,
    UNIQUE KEY uk_bucket_key (bucket, object_key),
    KEY           idx_tenant_uploader (tenant_id, uploaded_by)
);
```

- 业务侧**只引用 file_id**，不直接拼接 OSS URL
- 删除业务记录时**软删除**文件元数据，定时任务异步清理 OSS 对象

## 图片处理

- 用 MinIO Function Hooks / 异步任务生成缩略图
- 前端按需请求：`?w=200&h=200&fit=cover`
- **禁止**前端用 `<img>` 直接加载原图（流量浪费）

## 灾备

- 生产 MinIO 必须开启**多副本**（3+ 节点）或**跨区域复制**
- 关键 Bucket（用户头像、订单附件）每日异地备份
- 备份验证：每月做一次"恢复演练"

## 禁止清单

- 禁止用户上传文件名直接作为 Object Key
- 禁止生产环境永久签名 URL
- 禁止 `public-read` 私有数据
- 禁止接受可执行文件
- 禁止依赖前端传的 `Content-Type`（魔数验证）
- 禁止 base64 大文件存 DB
- 禁止文件经过应用服务器转发（用直传）
- 禁止跨租户/跨环境共用 Bucket
