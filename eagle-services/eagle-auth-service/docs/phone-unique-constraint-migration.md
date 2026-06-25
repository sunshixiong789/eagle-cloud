# auth_account.phone 唯一约束 —— 存量库迁移说明

## 背景

`auth_account.phone` 原为**非唯一**索引。`bindPhone` / `changePhone` / `register` 等流程在
`findByPhone` 预检与 `save` 之间存在 TOCTOU 竞态，并发把不同账号设为同一手机号时可能产生
**重复手机号凭据**。

修复分两层：

1. **应用层兜底**（已随代码合入）：`AccountApplicationService.savePhoneWithUniquenessGuard`
   捕获 `DataIntegrityViolationException` 并翻译为 `PHONE_ALREADY_BOUND`（409）。
2. **数据库唯一约束**（本说明）：`Account` 实体的 `idx_account_phone` 已改为 `unique = true`。
   应用层兜底**只有在 DB 真正存在唯一索引时才会触发**，因此存量库必须执行下面的 DDL。

## 为什么需要手动执行

- auth-service **未接入 Flyway**；`ddl-auto` dev=`update`、prod=`validate`。
- `validate` **不校验索引/约束**，prod 部署不会因实体注解变化而失败，但唯一性也**不会自动生效**。
- `update` 在**已存在**同名非唯一索引时不会把它改成唯一（Hibernate 不 alter 既有索引）。

所以**所有已存在的环境（dev/test/staging/prod）都需手动执行一次** DDL；仅全新空库由 `update` 自动建为唯一。

## 迁移步骤（PostgreSQL）

### 1. 排查存量重复（必须先做，结果应为空）

```sql
SELECT phone, count(*)
FROM auth_account
WHERE phone IS NOT NULL
GROUP BY phone
HAVING count(*) > 1;
```

- 有输出 → 先人工/脚本清洗（保留主账号，其余置空或合并），**清空重复后再建唯一索引**，否则建索引会失败。
- `phone IS NULL` 不参与唯一约束（PostgreSQL 默认多个 NULL 互不冲突），微信/淘宝未绑号用户不受影响。

### 2. 重建为唯一索引（同名，避免改实体注解）

```sql
-- CONCURRENTLY 不锁表（不能在事务块内执行；大表低峰期跑）
DROP INDEX IF EXISTS idx_account_phone;
CREATE UNIQUE INDEX CONCURRENTLY idx_account_phone ON auth_account (phone);
```

> 若表很小也可省略 `CONCURRENTLY` 直接 `CREATE UNIQUE INDEX`。

### 3. 验证

```sql
\d auth_account   -- 确认 idx_account_phone 为 UNIQUE
```

## 回滚

```sql
DROP INDEX IF EXISTS idx_account_phone;
CREATE INDEX idx_account_phone ON auth_account (phone);
```

并在代码侧将实体 `@Index(... unique = true)` 改回（应用层兜底 catch 留着无害——无唯一索引时不会触发）。

## 待办（本次未做）

- 验证码校验接口限流（`PUT/POST /accounts/{id}/phone`、`/accounts/password/reset`）防爆破，
  建议走网关 Sentinel 或 `RedisRateLimiter`（per-account / per-IP）。属安全增强，单独评估。
