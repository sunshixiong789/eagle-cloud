# eagle-elasticsearch-starter — Elasticsearch 检索（Repository + 查询构造器）

## 何时使用

- 全文检索（商品、文章、日志搜索）
- 聚合分析（统计、Top N）
- 高亮显示
- 复杂多条件 + 排序的列表查询

## 何时不要使用

- 简单关系查询（用 JPA 即可）
- 强一致性要求场景（ES 是近实时索引）
- 写多读少场景（ES 写性能弱于 RDBMS）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-elasticsearch-starter')
```

```yaml
spring.elasticsearch:
  uris: ${ES_URIS:http://localhost:9200}
  username: ${ES_USER:}
  password: ${ES_PASSWORD:}
  connection-timeout: 5s
  socket-timeout: 30s

eagle.elasticsearch:
  enabled: true
  default-page-size: 20
```

`ElasticSearchEnvironmentPostProcessor` 自动调整连接池等参数。

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `EagleDocument` | 文档基类（含 `@Id` + 审计字段） |
| `BaseElasticSearchRepository<T, ID>` | Repository 基类（继承扩展常用方法） |
| `EsSearchRequest` | 通用搜索入参（query / filters / sort / page / highlight） |
| `EsPageResult<T>` | 通用搜索响应（含 hits / aggregations） |
| `EsQueryBuilder` | 查询构造器（链式 DSL 简化） |
| `EsHighlightUtil` | 高亮处理 |
| `EsAggregationUtil` | 聚合查询 |

## 最小示例

```java
// 文档定义
@Document(indexName = "product")
@Getter
@Setter
public class ProductDoc extends EagleDocument {
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;
}

// Repository
public interface ProductSearchRepository extends BaseElasticSearchRepository<ProductDoc, String> {
}

// 查询服务
@RequiredArgsConstructor
@Service
public class ProductSearchService {

    private final ElasticsearchOperations operations;

    public EsPageResult<ProductDoc> search(EsSearchRequest req) {
        Query query = EsQueryBuilder.create()
            .matchPhrase("name", req.getKeyword())
            .term("category", req.getCategory())
            .range("price", req.getPriceMin(), req.getPriceMax())
            .sortDesc("createdAt")
            .highlight("name")
            .page(req.getPage(), req.getSize())
            .build();

        SearchHits<ProductDoc> hits = operations.search(query, ProductDoc.class);
        return EsPageResult.from(hits);
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.elasticsearch.enabled` | boolean | `true` | 总开关 |
| `eagle.elasticsearch.default-page-size` | int | `20` | 默认分页大小 |
| `eagle.elasticsearch.max-result-window` | int | `10000` | from + size 上限 |

## 常见错误

- ❌ 深翻页 `from > 10000` → ✅ 用 `search_after` 游标分页
- ❌ Text 字段做 term 精确匹配 → ✅ 用 `keyword` 子字段
- ❌ 写完立即读 → ✅ 等待 1s 或显式 `refresh`（生产慎用）
- ❌ 把 ES 当 OLTP 主库 → ✅ ES 是辅助检索，源数据在 RDBMS
- ❌ 用 ES 替代 SQL JOIN → ✅ 父子文档或宽表反范式

## 关联规则

- `.claude/rules/23-performance.md` — 翻页性能
- `.claude/rules/06-database.md` — ES 与 RDBMS 数据同步策略
