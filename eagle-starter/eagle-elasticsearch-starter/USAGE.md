# eagle-elasticsearch-starter — Elasticsearch 检索（流式查询构造器 + 高亮 + 通用 Repository）

## 何时使用

- 全文检索（商品 / 内容 / 日志）
- 多字段过滤 + 排序 + 高亮 + 聚合
- 列表搜索性能瓶颈下迁移到 ES

## 何时不要使用

- 简单关系查询（用 JPA / MyBatis）
- 强一致性场景（ES 是近实时索引）
- 写多读少场景

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-elasticsearch-starter')
```

```yaml
eagle.elasticsearch:
  uris:
    - http://localhost:9200
  username: ${ES_USER:}
  password: ${ES_PASSWORD:}
  connect-timeout: 5000             # ms
  socket-timeout: 30000              # ms
  ssl-enabled: false
```

`ElasticSearchEnvironmentPostProcessor` 自动桥接到 Spring Data ES 原生 `spring.elasticsearch.*` 属性。

## 核心 API

| 类                                                      | 用途                                                                                                               |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `EagleDocument`                                        | 文档基类（含 `@Id` + 审计字段）                                                                                             |
| `BaseElasticSearchRepository<T extends EagleDocument>` | Repository 基类：`save / saveAll(自动分批 500) / findById / deleteById / search(NativeQuery) / count`                   |
| `EsSearchRequest`                                      | 通用搜索入参                                                                                                           |
| `EsPageResult<T>`                                      | 通用分页响应（含 hits）                                                                                                   |
| `EsQueryBuilder`                                       | **静态 `of()` + 链式构造**：`multiMatch / term / terms / range / prefix / wildcard / sort / highlight / page / build()` |
| `EsHighlightUtil`                                      | 高亮反射写回（`search()` 自动调用）                                                                                          |
| `EsAggregationUtil`                                    | 聚合查询辅助                                                                                                           |

## 最小示例

```java
// 文档定义
@Document(indexName = "product")
@Getter
@Setter
public class ProductDoc extends EagleDocument {

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;
}

// Repository（继承基类，传入 ElasticsearchOperations 和 Class）
@Repository
public class ProductRepository extends BaseElasticSearchRepository<ProductDoc> {

    public ProductRepository(ElasticsearchOperations operations) {
        super(operations, ProductDoc.class);
    }

    public EsPageResult<ProductDoc> search(String keyword, String category,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           int page, int size) {
        NativeQuery query = EsQueryBuilder.of()
                .multiMatch(keyword, "title", "description")
                .term("category", category)
                .range("price", minPrice, maxPrice)
                .sort("salesCount", SortOrder.Desc)
                .highlight("title", "description")
                .page(page, size)
                .build();
        return search(query);   // 基类方法，自动应用高亮
    }
}

// 写入
productRepository.

save(new ProductDoc(...));
        productRepository.

saveAll(List.of(doc1, doc2, ...));   // 自动按 500 分批

// 删除
        productRepository.

deleteById("doc-id");

// 统计
long total = productRepository.count(query);
```

## 配置项

| key                                   | 类型      | 默认                        | 说明            |
|---------------------------------------|---------|---------------------------|---------------|
| `eagle.elasticsearch.uris`            | List    | `[http://localhost:9200]` | 节点地址          |
| `eagle.elasticsearch.username`        | String  | —                         | 用户名           |
| `eagle.elasticsearch.password`        | String  | —                         | 密码            |
| `eagle.elasticsearch.connect-timeout` | int     | `5000`                    | 连接超时（ms）      |
| `eagle.elasticsearch.socket-timeout`  | int     | `30000`                   | Socket 超时（ms） |
| `eagle.elasticsearch.ssl-enabled`     | boolean | `false`                   | 启用 SSL        |

## EsQueryBuilder 全部方法

| 方法                                | 子句         | 说明                        |
|-----------------------------------|------------|---------------------------|
| `multiMatch(keyword, fields...)`  | must       | 多字段全文，参与评分                |
| `term(field, value)`              | filter     | 精确匹配，不评分                  |
| `terms(field, values...)`         | filter     | 多值精确匹配                    |
| `range(field, min, max)`          | filter     | 范围（min/max 任一为 null 则不限边） |
| `prefix(field, prefix)`           | filter     | 前缀（自动补全）                  |
| `wildcard(field, pattern)`        | filter     | 通配符（性能差，慎用大索引）            |
| `sort(field, SortOrder.Asc/Desc)` | sort       | 多字段排序按调用顺序                |
| `highlight(fields...)`            | highlight  | 默认 `<em>...</em>` 标签      |
| `page(page, size)`                | pagination | page 从 1 开始               |
| `build()`                         | —          | 返回 `NativeQuery`          |

## 常见错误

- ❌ Text 字段做 term 精确匹配 → ✅ 用 `keyword` 子字段或 `term(field.keyword, ...)`
- ❌ 深翻页 `page > 100` 用 `page()` → ✅ 用 `search_after` 游标
- ❌ 写完立即读 → ✅ 等 1s 或显式 `refresh`（生产慎用）
- ❌ `EsQueryBuilder` 实例复用 → ✅ 每次 `EsQueryBuilder.of()` 新建（有状态）
- ❌ 配置写 `eagle.elasticsearch.enabled` → ✅ 没有此字段
- ❌ Repository 继承 Spring Data ES 的 `ElasticsearchRepository` → ✅ 继承 **`BaseElasticSearchRepository`**

## 关联规则

- `.claude/rules/04-data.md` — 翻页 / 索引性能 / ES 与 RDBMS 同步策略
