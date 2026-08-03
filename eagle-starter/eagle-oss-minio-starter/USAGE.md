# eagle-oss-minio-starter — 对象存储（MinIO / 本地文件系统）

## 何时使用

- 用户上传文件（图片、视频、附件）
- 头像 / Logo / 产品图
- 富文本嵌入图片

## 何时不要使用

- 配置文件 / 字典（用 DB）
- 极小数据（< 4KB，存 DB）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-oss-minio-starter')
```

```yaml
eagle.storage:
  type: minio                          # local / minio / oss
  minio:
    endpoint: ${MINIO_URL:http://localhost:9000}
    access-key: ${MINIO_AK}
    secret-key: ${MINIO_SK}
  local:
    base-path: /data/eagle/storage     # type=local 用
    url-prefix: http://localhost:8080/storage
```

⚠️ **`type` 默认是 `local`**——单机/开发期友好。生产必须切到 `minio`。

## 核心 API

```java
public interface StorageService {
    String upload(String bucket, String path, InputStream input, long size, String mimeType);

    InputStream download(String bucket, String path);

    void delete(String bucket, String path);

    String getUrl(String bucket, String path);
}
```

| 实现                        | 启用条件         |
|---------------------------|--------------|
| `MinioStorageServiceImpl` | `type=minio` |
| `LocalStorageServiceImpl` | `type=local` |

⚠️ **接口仅 4 个方法**，**没有** `presignedGetUrl / presignedPutUrl / exists`。需要签名直传需自行扩展。

## 最小示例

```java

@RequiredArgsConstructor
@Service
public class FileApplicationService {

    private final StorageService storage;

    /** 普通上传：业务上传 → 后端转存 → 返回 URL */
    public FileResponse upload(MultipartFile file, Long userId) {
        validate(file);                                       // 大小、后缀、MIME 校验
        String tenantId = TenantContextHolder.getTenantId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = String.format("%s/avatar/%s/%s.%s",
                tenantId, date, UUID.randomUUID(), ext(file.getOriginalFilename()));

        try (InputStream in = file.getInputStream()) {
            String url = storage.upload(
                    "eagle-prod-user-avatar",
                    objectKey,
                    in,
                    file.getSize(),
                    file.getContentType()
            );
            return new FileResponse(objectKey, url);
        }
    }

    /** 下载（流式）*/
    public StreamingResponseBody download(String bucket, String key) {
        InputStream in = storage.download(bucket, key);
        return out -> in.transferTo(out);
    }

    /** 软删除：业务表标记 + 异步删 OSS */
    public void delete(String bucket, String key) {
        storage.delete(bucket, key);
    }
}
```

## 配置项

| key                              | 类型     | 默认                              | 说明                    |
|----------------------------------|--------|---------------------------------|-----------------------|
| `eagle.storage.type`             | String | **`local`**                     | `local / minio / oss` |
| `eagle.storage.minio.endpoint`   | String | `http://localhost:9000`         | MinIO 地址              |
| `eagle.storage.minio.access-key` | String | —                               | AK（生产 ENC()）          |
| `eagle.storage.minio.secret-key` | String | —                               | SK（生产 ENC()）          |
| `eagle.storage.local.base-path`  | String | `/data/eagle/storage`           | 本地存储根目录               |
| `eagle.storage.local.url-prefix` | String | `http://localhost:8080/storage` | 访问 URL 前缀             |

⚠️ **没有 `enabled` / `upload.max-size` / `upload.allowed-extensions` 等**——业务方需自行做上传校验。

## Bucket / Key 设计建议

业务方约定：

- Bucket 命名：`eagle-{env}-{purpose}`（`eagle-prod-user-avatar`）
- Key 命名：`{tenantId}/{biz}/{yyyy/MM/dd}/{uuid}.{ext}`
- **强制 UUID 重命名**（防路径穿越 / 防猜测）
- 上传校验（大小 / 后缀白名单 / MIME 真实性）由业务层实现

## 常见错误

- ❌ 期望默认 `type=minio` → ✅ **默认是 `local`**
- ❌ 调用 `storage.presignedGetUrl(...)` → ✅ 接口没有此方法，需自行扩展
- ❌ Key 用原始文件名 → ✅ UUID 重命名
- ❌ 大文件经过应用服务器 → ✅ 自定义大文件直传（接口需扩展）
- ❌ 信任前端 `Content-Type` → ✅ 用 Apache Tika 检测真实 MIME
- ❌ AK/SK 明文 → ✅ ENC() 加密

## 关联规则

- `.claude/rules/05-security.md` — 上传安全 / 多租户隔离
