---
name: eagle-sharding
description: Use when configuring database sharding in eagle-cloud with Apache ShardingSphere 5.5.0 — YAML-driven sharding rules, ShardingProperties, data source routing, sharding key design, read-write splitting integration with eagle-dynamic-datasource-starter
---

# eagle-sharding-starter — 分库分表（Apache ShardingSphere 5.5.0）

## 何时使用

- 单表数据量 > 5000 万行，或预计 1 年内达到此量级
- 写 QPS 超出单库承载能力
- 需要按租户或业务维度物理隔离数据

## 何时不要使用

- 数据量 < 1000 万，先考虑索引优化 + 读写分离
- 业务有大量跨分片联表查询（分片键设计不合理）
- 分片 JOIN 无法避免时 → 降级方案：应用层聚合

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-sharding-starter')
```

```yaml
eagle:
  sharding:
    enabled: true
    config-file: classpath:sharding.yaml    # ShardingSphere YAML 配置文件路径
```

分片规则在独立的 `sharding.yaml` 中声明（ShardingSphere 原生格式），starter 负责加载并创建 `ShardingSphereDataSource`。

## `sharding.yaml` 结构（最小示例）

```yaml
# src/main/resources/sharding.yaml
dataSources:
  ds_0:
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://db0:3306/eagle_order
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  ds_1:
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://db1:3306/eagle_order
    username: ${DB_USER}
    password: ${DB_PASSWORD}

rules:
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds_${0..1}.t_order_${0..3}   # 2 库 × 4 表 = 8 分片
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: order_table_hash
        databaseStrategy:
          standard:
            shardingColumn: tenant_id
            shardingAlgorithmName: tenant_db_hash
        keyGenerateStrategy:
          column: order_id
          keyGeneratorName: snowflake

    shardingAlgorithms:
      order_table_hash:
        type: HASH_MOD
        props:
          sharding-count: 4
      tenant_db_hash:
        type: HASH_MOD
        props:
          sharding-count: 2

    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: false   # 生产关闭；开发期可设 true 打印路由结果
```

## 分片键设计原则

```
✅ 按高频查询维度分片（订单按 user_id，日志按 tenant_id）
✅ 分片键选择区分度高、分布均匀的字段
✅ 多租户场景：库分片用 tenant_id，表分片用业务 ID

❌ 禁止用时间字段做唯一分片键（热点写入单片）
❌ 禁止用枚举/状态做分片键（区分度低）
❌ 跨分片 JOIN / 子查询（性能灾难）
```

## 与读写分离集成

`eagle-sharding-starter` 可与 `eagle-dynamic-datasource-starter` 配合：分片策略在 ShardingSphere 层，主从路由在 Dynamic DataSource 层。

```yaml
# sharding.yaml 内声明读写分离
rules:
  - !READWRITE_SPLITTING
    dataSources:
      rw_ds_0:
        staticStrategy:
          writeDataSourceName: ds_0_write
          readDataSourceNames:
            - ds_0_read_0
            - ds_0_read_1
```

## 分布式主键

ShardingSphere 内置 `SNOWFLAKE` 生成器，业务代码无需手动调用 `eagle-id-generator-starter`（两者选一）：

```yaml
keyGenerators:
  snowflake:
    type: SNOWFLAKE
    props:
      worker-id: ${WORKER_ID:1}
```

若已有 `eagle-id-generator-starter`，则在 `sharding.yaml` 不声明 `keyGenerateStrategy`，由业务层显式赋 ID。

## 常见错误

- ❌ `actualDataNodes` 表名写错导致路由失败 → ✅ `sql-show: true` 看实际路由
- ❌ 分片键不在 WHERE 子句 → ✅ ShardingSphere 会全路由（广播查询）性能极差
- ❌ 跨分片 `ORDER BY + LIMIT` → ✅ 结果合并代价高，改游标分页或限制 OFFSET
- ❌ 事务跨多个分片数据源 → ✅ ShardingSphere XA / Seata AT 集成，需额外配置

## 关联规则

- `.claude/rules/06-database.md` — 索引 / 物理 FK 禁止
- `.claude/rules/16-transaction-distributed.md` — 跨分片事务
- `.claude/rules/23-performance.md` — 大数据量分页
