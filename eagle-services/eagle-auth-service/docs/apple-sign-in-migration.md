# Apple Sign In 账号绑定 —— 存量库迁移说明

## 背景

Apple 登录在 `auth_account` 增加两个可空字段：

- `apple_subject`：服务端验签后的 Apple identity token `sub`，全局唯一；
- `apple_bind_time`：首次绑定时间。

auth-service 尚未接入 Flyway，dev 使用 `ddl-auto=update`，prod 使用
`ddl-auto=validate`。因此存量生产库必须在发布新应用代码前手动执行本页 DDL，
否则 Hibernate 启动校验会因缺列失败。

## 上线步骤（PostgreSQL）

### 1. 先加可空列

```sql
ALTER TABLE auth_account
    ADD COLUMN IF NOT EXISTS apple_subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS apple_bind_time TIMESTAMP;

COMMENT ON COLUMN auth_account.apple_subject IS 'Apple Sign In subject';
COMMENT ON COLUMN auth_account.apple_bind_time IS 'Apple 绑定时间';
```

可空列对旧版本应用保持向后兼容，可先于应用代码发布。

### 2. 建唯一索引

```sql
-- CONCURRENTLY 不能放在事务块内执行；建议业务低峰期单独运行。
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_account_apple_subject
    ON auth_account (apple_subject);
```

PostgreSQL 唯一索引允许多行 `NULL`，已有手机号、微信、淘宝账号不受影响。

### 3. 验证

```sql
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'auth_account'
  AND column_name IN ('apple_subject', 'apple_bind_time');

SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'auth_account'
  AND indexname = 'idx_account_apple_subject';
```

确认两列存在，且索引定义包含 `CREATE UNIQUE INDEX` 后再发布 auth-service。

## 回滚

应用代码回滚后，新列不会影响旧版本读写。观察期结束且确认不再需要数据时，
再执行以下高危 DDL：

```sql
DROP INDEX IF EXISTS idx_account_apple_subject;
ALTER TABLE auth_account
    DROP COLUMN IF EXISTS apple_bind_time,
    DROP COLUMN IF EXISTS apple_subject;
```

不要在应用回滚的同一时间立即删列，以免滚动发布期间仍有新版本实例访问这些字段。
