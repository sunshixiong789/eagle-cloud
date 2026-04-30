# 数据库迁移规范（Flyway）

技术栈：Flyway（推荐）/ Liquibase 二选一，本项目以 Flyway 为基线。

**核心原则**：生产环境**禁止 `ddl-auto: update`**，所有 DDL/DML 通过迁移脚本受版本控制。

## 文件位置

```
{module}/src/main/resources/db/migration/
├── V202604301030__init_user.sql
├── V202604301045__add_user_avatar.sql
├── V202604301100__seed_default_roles.sql
└── R__update_views.sql            # Repeatable（仅视图 / 函数等）
```

每个**业务模块**（不是每个服务）维护各自的迁移文件，避免合并冲突。

## 命名规范

格式：`V{yyyyMMddHHmm}__{snake_case_description}.sql`

```
V202604301030__create_t_user.sql              # 建表
V202604301045__alter_t_user_add_avatar.sql    # 加列
V202604301100__create_idx_user_email.sql      # 加索引
V202604301115__seed_dict_payment_status.sql   # 数据初始化
V202604301130__migrate_legacy_status.sql      # 数据迁移
V202604301145__drop_t_user_legacy.sql         # 删除（高危）
```

- 版本号用**时间戳**（`yyyyMMddHHmm`），保证全局递增、避免合并冲突
- 描述用 `snake_case`，动词开头（`create / alter / drop / seed / migrate / rename`）
- **禁止**用顺序数字（`V1__... V2__...`），多人开发必撞号

## 不可变性（铁律）

**已发布到任何环境（含 dev）的迁移文件不得修改**：

```bash
# ✅ 修改逻辑请新建迁移
V202604301030__create_t_user.sql      # 已合并，不动
V202604301400__alter_t_user_fix.sql   # 新建修复脚本

# ❌ 严禁：直接改已合并的脚本
```

修改已发布脚本会导致 Flyway 校验失败（checksum 不匹配），所有环境启动失败。

## 内容规范

### DDL 脚本

```sql
-- V202604301030__create_t_order.sql
CREATE TABLE t_order (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    tenant_id     VARCHAR(64)  NOT NULL                 COMMENT '租户ID',
    order_no      VARCHAR(32)  NOT NULL                 COMMENT '订单号',
    status        VARCHAR(20)  NOT NULL DEFAULT 'CREATED' COMMENT '状态',
    total_amount  DECIMAL(18,2) NOT NULL DEFAULT 0      COMMENT '订单金额',
    version       INT          NOT NULL DEFAULT 0       COMMENT '乐观锁',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(64),
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    VARCHAR(64),
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_tenant_status_created (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

- 表名前缀 `t_`，字段全小写、下划线分隔
- 每个字段必须 `COMMENT`
- 多租户表 `tenant_id` 前导列入索引
- 必带审计 4 字段（`created_at / created_by / updated_at / updated_by`）+ `deleted` 软删除
- 聚合根表必须有 `version` 列（乐观锁）

### 加列（向后兼容）

```sql
-- ✅ 新增列必须有默认值，不影响旧代码读写
ALTER TABLE t_user ADD COLUMN avatar VARCHAR(500) NULL COMMENT '头像URL';

-- ❌ 禁止：NOT NULL 无默认值（旧代码 INSERT 失败）
ALTER TABLE t_user ADD COLUMN avatar VARCHAR(500) NOT NULL;
```

### 加索引（在线 DDL）

```sql
-- MySQL 8 在线建索引（不锁表）
ALTER TABLE t_order ADD INDEX idx_user_status (user_id, status), ALGORITHM=INPLACE, LOCK=NONE;
```

百万行以上表加索引需评估业务低峰期执行。

### 数据迁移

```sql
-- ✅ 大表分批，避免长事务
UPDATE t_order SET status = 'COMPLETED'
WHERE status = 'CLOSED' AND id BETWEEN 1 AND 100000;
-- 多个文件分批，或脚本内循环
```

数据迁移**不要**直接 `UPDATE 全表`，按主键分批 1–10 万行/批，避免长事务 + binlog 暴涨。

### 高危操作

| 操作 | 风险 | 缓解 |
|------|------|------|
| `DROP COLUMN` | 旧版本读写报错 | 多步：先停用代码读写 → 下次发布删字段 |
| `RENAME` | 旧代码找不到 | 改为新增 + 数据迁移 + 后续删除 |
| `DROP TABLE` | 不可逆 | 先重命名为 `t_xxx_archive_yyyymmdd` 观察 N 天 |
| `ALTER 大表加非空列` | 锁表 | 先加可空 + 回填 + 改非空，分多个迁移 |

## 配置

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true        # 已有数据库首次接入
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false              # 严禁乱序应用
  jpa:
    hibernate:
      ddl-auto: validate              # 生产强制 validate；dev 可用 none
```

- 生产 `ddl-auto: validate`（启动期校验实体与表结构匹配）
- 开发期允许 `none`，**禁止** `update / create / create-drop`

## 多模块协作

不同业务模块的迁移文件通过**不同 schema 前缀**避免冲突：

```
eagle-system-server/db/migration/V*_sys_*.sql
eagle-order-server/db/migration/V*_ord_*.sql
```

或时间戳天然不冲突即可。

## 回滚策略

Flyway **不支持自动回滚**（与 Liquibase 不同），回滚需**前向修复**：

```sql
-- 反向修复脚本
V202604301600__rollback_user_avatar.sql:
ALTER TABLE t_user DROP COLUMN avatar;
```

- 上线前**必须**编写回滚 SQL（PR 描述里附）
- DDL 回滚优先用"新建反向脚本"，**禁止**手工删除迁移记录

## 测试

- 单元测试用 H2 / Testcontainers MySQL，自动应用 Flyway
- CI 阶段在每次 PR 启动新 DB 容器跑全量迁移，确保新脚本可正常应用

## 禁止清单

- 禁止生产开启 `ddl-auto: update`
- 禁止修改已发布迁移文件
- 禁止迁移文件包含业务逻辑（应用层处理，迁移仅做结构 + 字典初始化）
- 禁止迁移用顺序数字版本号（必撞号）
- 禁止 `DROP TABLE / DROP COLUMN` 不留过渡期
- 禁止单个迁移脚本 > 1MB（拆分多文件）
- 禁止迁移与业务代码同 PR（DB 变更 PR 先合并并发布到所有环境，再合代码 PR）
