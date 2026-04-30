# eagle-oss-minio-starter — 对象存储（MinIO 主 / 本地降级）

## 何时使用

- 用户上传文件（图片、视频、附件）
- 临时大对象（导出报表、PDF）
- 富文本嵌入图片
- 头像 / Logo / 产品图

## 何时不要使用

- 配置文件 / 字典（用 DB / Nacos）
- < 4KB 的小数据（用 DB）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-oss-minio-starter')
```

```yaml
eagle.storage:
  enabled: true
  type: minio                          # minio / local
  minio:
    endpoint: ${MINIO_URL:http://localhost:9000}
    access-key: ${MINIO_AK}
    secret-key: ${MINIO_SK}
    default-bucket: eagle-prod-public
  local:
    base-path: /data/eagle/uploads     # type=local 时使用（开发期 / 单机）
  upload:
    max-size: 50MB
    allowed-extensions:
      - jpg
      - png
      - pdf
      - docx
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `StorageService` | 存储抽象（`upload` / `download` / `presignedGetUrl` / `presignedPutUrl` / `remove` / `exists`）|
| `MinioStorageServiceImpl` | MinIO 实现 |
| `LocalStorageServiceImpl` | 本地文件系统实现（开发期 / 降级） |
| `StorageProperties` | 配置项 |

## 最小示例

```java
@RequiredArgsConstructor
@Service
public class FileApplicationService {
    private final StorageService storage;

    /** 直接上传（小文件 < 5MB）*/
    public FileResponse upload(MultipartFile file, Long userId) {
        validateFile(file);
        String key = buildKey(userId, file);
        storage.upload("eagle-prod-user-avatar", key, file.getInputStream(), file.getSize());

        return FileResponse.builder()
            .bucket("eagle-prod-user-avatar")
            .key(key)
            .url(storage.presignedGetUrl("eagle-prod-user-avatar", key, Duration.ofHours(1)))
            .build();
    }

    /** 大文件直传（推荐）：返回签名 URL，浏览器 PUT 直传 */
    public UploadTicket getUploadTicket(Long userId, String filename) {
        String key = userId + "/" + UUID.randomUUID() + "." + ext(filename);
        String uploadUrl = storage.presignedPutUrl("eagle-prod-user-avatar", key, Duration.ofMinutes(10));
        return new UploadTicket(uploadUrl, key);
    }

    /** 下载（短 TTL 签名 URL）*/
    public String getDownloadUrl(String bucket, String key) {
        if (!storage.exists(bucket, key)) {
            throw FileErrorCode.FILE_NOT_FOUND.toNotFoundException();
        }
        return storage.presignedGetUrl(bucket, key, Duration.ofMinutes(15));
    }
}

private String buildKey(Long userId, MultipartFile file) {
    String tenantId = TenantContextHolder.getCurrentTenantId();
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    return String.format("%s/avatar/%s/%s.%s", tenantId, date,
        UUID.randomUUID(), ext(file.getOriginalFilename()));
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.storage.enabled` | boolean | `true` | 总开关 |
| `eagle.storage.type` | enum | `minio` | `minio` / `local` |
| `eagle.storage.minio.endpoint` | String | — | MinIO 服务地址 |
| `eagle.storage.minio.access-key` | String | — | AK（生产 ENC()）|
| `eagle.storage.minio.secret-key` | String | — | SK（生产 ENC()）|
| `eagle.storage.upload.max-size` | DataSize | `50MB` | 上传大小限制 |
| `eagle.storage.upload.allowed-extensions` | List | jpg/png/pdf/docx | 后缀白名单 |

## 常见错误

- ❌ Key 用原始文件名 → ✅ UUID 重命名（防路径穿越 / 防猜测）
- ❌ Bucket 全设 `public-read` → ✅ 默认私有 + 签名 URL
- ❌ 永久签名 URL → ✅ TTL ≤ 1 小时
- ❌ 用户文件经过应用服务器转发（大文件）→ ✅ 直传签名（`presignedPutUrl`）
- ❌ 信任前端 `Content-Type` → ✅ Apache Tika 检测真实 MIME
- ❌ 多租户共用 Key 命名 → ✅ Key 必须以 `tenantId` 开头

## 关联规则

- `.claude/rules/26-file-storage.md` — Bucket / Key 设计 / 上传校验 / 病毒扫描
- `.claude/rules/12-security.md` — 文件上传安全
- `.claude/rules/17-tenant-permission.md` — 多租户文件隔离
