---
name: eagle-encrypt
description: Use when encrypting sensitive JPA entity fields in eagle-cloud — EncryptedStringConverter with @Convert annotation, AES-256 field-level encryption/decryption via EncryptionService, transparent passthrough when secret-key not configured
---

# eagle-encrypt-starter — JPA 字段级加密（AES-256）

## 何时使用

- 数据库中存储手机号、身份证号、银行卡号、地址等敏感字段
- 需要满足等保 / GDPR 对静态数据加密的合规要求
- 字段加密后对业务代码透明（读写时自动加解密）

## 何时不要使用

- 密码字段 → 用 BCrypt 单向哈希（`@Convert` 加密是双向的，不适合密码）
- 全表加密 → 性能代价高，改用数据库 TDE
- Token / Session 存储 → 属于 Auth 层关注点

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-encrypt-starter')
```

```yaml
eagle:
  encrypt:
    secret-key: ${EAGLE_ENCRYPT_SECRET_KEY}   # 32 字节 AES-256 密钥（Base64 编码）
    # 不配置时 EncryptionService 不注册，EncryptedStringConverter 原样透传（开发期方便）
```

生产环境密钥通过环境变量注入，**禁止**明文写入 yml 文件（用 Jasypt `ENC()` 或 K8s Secret）。

## 核心 API

| 类                         | 说明                                                          |
|--------------------------|-------------------------------------------------------------|
| `EncryptedStringConverter` | JPA `AttributeConverter`，标注在实体字段上 `@Convert(converter = EncryptedStringConverter.class)` |
| `EncryptionService`        | 接口：`encrypt(String)` / `decrypt(String)`                   |
| `AesEncryptionService`     | 默认实现，AES-256-GCM，`@ConditionalOnProperty(eagle.encrypt.secret-key)` |

## 最小示例

```java
// 1) 实体字段标注 @Convert
@Entity
@Table(name = "t_customer")
@Getter
@NoArgsConstructor
public class Customer extends BaseAggregateRoot<Customer> {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "mobile", comment = "手机号（AES 加密存储）")
    private String mobile;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "id_card", comment = "身份证号（AES 加密存储）")
    private String idCard;

    @Column(name = "name", comment = "姓名（明文）")
    private String name;

    public static Customer create(String name, String mobile, String idCard) {
        Customer c = new Customer();
        c.name = name;
        c.mobile = mobile;    // 存入 DB 时自动加密
        c.idCard = idCard;
        return c;
    }
}

// 2) 使用（加解密完全透明）
Customer customer = customerRepository.findById(id).orElseThrow();
String plainMobile = customer.getMobile();   // 读出时已自动解密
```

## 注意事项

- **加密字段不可作为查询条件**（`WHERE mobile = ?` 会因密文不同而失败）。需要按手机号查询时，另维护一个 `mobile_hash` 字段（`SHA-256(mobile + salt)`）用于等值索引。
- `EncryptedStringConverter` 用 `@Autowired(required = false)` 注入 `EncryptionService`——未配置密钥时原样返回，便于开发环境跑测试。
- 密钥轮换：旧密钥解密 → 新密钥加密 → 批量写回，需在业务低峰期通过迁移脚本完成。

```java
// 按手机号查询的正确方式（哈希索引）
@Column(name = "mobile_hash", comment = "手机号哈希（用于等值查询）")
private String mobileHash;

// 写入时
customer.mobileHash = DigestUtils.sha256Hex(mobile + salt);
// 查询时
customerRepository.findByMobileHash(DigestUtils.sha256Hex(mobile + salt));
```

## 常见错误

- ❌ 加密字段直接 `WHERE mobile = ?` → ✅ 维护 `mobile_hash` 列做等值查询
- ❌ 生产 yml 明文写 `secret-key: abc123` → ✅ 环境变量或 Jasypt `ENC()` 注入
- ❌ 开发期不配置密钥导致生产数据混乱 → ✅ dev/prod profile 分别配置，dev 可用固定测试密钥

## 关联规则

- `.claude/rules/12-security.md` — 敏感字段存储规范
- `.claude/rules/19-config.md` — Jasypt 加密配置
- `.claude/rules/06-database.md` — 字段注释规范
