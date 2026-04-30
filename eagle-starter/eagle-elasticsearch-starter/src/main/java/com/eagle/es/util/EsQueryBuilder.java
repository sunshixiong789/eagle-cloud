package com.eagle.es.util;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 查询构建工具。
 *
 * <p>提供流式（Builder）API，封装 Spring Data Elasticsearch 的 {@link NativeQuery} 构建逻辑，
 * 简化常见查询场景（多字段全文、精确匹配、范围过滤、排序、高亮、分页）的实现。
 *
 * <p>使用示例：
 * <pre>{@code
 * NativeQuery query = EsQueryBuilder.of()
 *     .multiMatch("iPhone 15", "title", "description")
 *     .term("category", "electronics")
 *     .range("price", 100, 5000)
 *     .sort("salesCount", SortOrder.Desc)
 *     .highlight("title", "description")
 *     .page(1, 20)
 *     .build();
 * }</pre>
 *
 * <p>此类为有状态构建器，每次通过 {@link #of()} 获取新实例，禁止多线程共享同一实例。
 *
 * @author eagle
 */
public class EsQueryBuilder {

    /**
     * 布尔查询构建器，所有条件均追加到 must / filter / should 子句
     */
    private final BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

    /**
     * 排序选项列表，顺序即优先级
     */
    private final List<SortOptions> sorts = new ArrayList<>();

    /**
     * 高亮字段列表
     */
    private final List<HighlightField> highlights = new ArrayList<>();

    /**
     * 分页起始偏移量（from），默认第 1 页
     */
    private int from = 0;

    /**
     * 每页大小
     */
    private int size = 20;

    /**
     * 私有构造函数，通过 {@link #of()} 创建实例。
     */
    private EsQueryBuilder() {
    }

    /**
     * 创建新的查询构建器实例。
     *
     * @return 新的 {@link EsQueryBuilder} 实例
     */
    public static EsQueryBuilder of() {
        return new EsQueryBuilder();
    }

    /**
     * 添加多字段全文搜索（multi_match）。
     *
     * <p>当 keyword 为空时，此条件不生效（不向查询中添加任何内容）。
     * 生成的查询为 must 子句，即全文搜索词是必须匹配的条件。
     *
     * @param keyword 搜索关键词
     * @param fields  目标字段名列表；为空则对所有字段搜索
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder multiMatch(String keyword, String... fields) {
        if (keyword == null || keyword.isBlank()) {
            return this;
        }
        Query multiMatchQuery = Query.of(q -> q
                .multiMatch(mm -> {
                    mm.query(keyword);
                    if (fields != null && fields.length > 0) {
                        mm.fields(Arrays.asList(fields));
                    }
                    return mm;
                })
        );
        boolBuilder.must(multiMatchQuery);
        return this;
    }

    /**
     * 添加精确匹配过滤条件（term）。
     *
     * <p>生成的查询为 filter 子句（不影响评分，性能更优）。
     * value 的字符串形式通过 {@link Object#toString()} 获取。
     *
     * @param field 字段名
     * @param value 精确匹配值
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder term(String field, Object value) {
        if (field == null || field.isBlank() || value == null) {
            return this;
        }
        Query termQuery = Query.of(q -> q
                .term(t -> t.field(field).value(value.toString()))
        );
        boolBuilder.filter(termQuery);
        return this;
    }

    /**
     * 添加多值精确匹配过滤条件（terms）。
     *
     * <p>生成的查询为 filter 子句。values 中每个元素通过 {@link Object#toString()} 转换。
     *
     * @param field  字段名
     * @param values 候选值列表；为空时不添加此条件
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder terms(String field, Object... values) {
        if (field == null || field.isBlank() || values == null || values.length == 0) {
            return this;
        }
        List<co.elastic.clients.elasticsearch._types.FieldValue> fieldValues = Arrays.stream(values)
                .filter(v -> v != null)
                .map(v -> co.elastic.clients.elasticsearch._types.FieldValue.of(v.toString()))
                .collect(Collectors.toList());
        if (fieldValues.isEmpty()) {
            return this;
        }
        Query termsQuery = Query.of(q -> q
                .terms(t -> t.field(field).terms(tv -> tv.value(fieldValues)))
        );
        boolBuilder.filter(termsQuery);
        return this;
    }

    /**
     * 添加范围过滤条件（range）。
     *
     * <p>生成的查询为 filter 子句（gte min，lte max）。
     * min / max 任意一方为 {@code null} 时，对应边界不设置。
     *
     * @param field 字段名
     * @param min   范围下界（gte），{@code null} 表示不限
     * @param max   范围上界（lte），{@code null} 表示不限
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder range(String field, Object min, Object max) {
        if (field == null || field.isBlank() || (min == null && max == null)) {
            return this;
        }
        Query rangeQuery = Query.of(q -> q
                .range(r -> r.untyped(u -> {
                    u.field(field);
                    if (min != null) {
                        u.gte(co.elastic.clients.json.JsonData.of(min));
                    }
                    if (max != null) {
                        u.lte(co.elastic.clients.json.JsonData.of(max));
                    }
                    return u;
                }))
        );
        boolBuilder.filter(rangeQuery);
        return this;
    }

    /**
     * 添加前缀查询（prefix）。
     *
     * <p>适用于自动补全等场景，生成 filter 子句。
     *
     * @param field  字段名
     * @param prefix 前缀字符串
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder prefix(String field, String prefix) {
        if (field == null || field.isBlank() || prefix == null || prefix.isBlank()) {
            return this;
        }
        Query prefixQuery = Query.of(q -> q
                .prefix(p -> p.field(field).value(prefix))
        );
        boolBuilder.filter(prefixQuery);
        return this;
    }

    /**
     * 添加通配符查询（wildcard）。
     *
     * <p>支持 {@code ?}（单字符）和 {@code *}（零或多字符）通配符，生成 filter 子句。
     * 注意：通配符查询性能较差，避免在大索引中使用。
     *
     * @param field   字段名
     * @param pattern 通配符模式
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder wildcard(String field, String pattern) {
        if (field == null || field.isBlank() || pattern == null || pattern.isBlank()) {
            return this;
        }
        Query wildcardQuery = Query.of(q -> q
                .wildcard(w -> w.field(field).value(pattern))
        );
        boolBuilder.filter(wildcardQuery);
        return this;
    }

    /**
     * 添加排序字段。
     *
     * <p>可多次调用以添加多字段排序，顺序即优先级。
     *
     * @param field 排序字段名
     * @param order 排序方向：{@link SortOrder#Asc} 或 {@link SortOrder#Desc}
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder sort(String field, SortOrder order) {
        if (field == null || field.isBlank() || order == null) {
            return this;
        }
        SortOptions sortOptions = SortOptions.of(s -> s
                .field(f -> f.field(field).order(order))
        );
        sorts.add(sortOptions);
        return this;
    }

    /**
     * 添加高亮字段。
     *
     * <p>高亮标签默认为 {@code <em>} / {@code </em>}，结果由
     * {@link EsHighlightUtil#applyHighlight(org.springframework.data.elasticsearch.core.SearchHit)}
     * 回写至文档对象的对应字段。
     *
     * @param fields 需要高亮的字段名列表
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder highlight(String... fields) {
        if (fields == null || fields.length == 0) {
            return this;
        }
        for (String field : fields) {
            if (field != null && !field.isBlank()) {
                HighlightField highlightField = new HighlightField(
                        field,
                        HighlightFieldParameters.builder().build()
                );
                highlights.add(highlightField);
            }
        }
        return this;
    }

    /**
     * 设置分页参数。
     *
     * @param page 当前页码，从 1 开始；传入小于 1 的值时重置为第 1 页
     * @param size 每页大小；传入小于 1 的值时重置为默认值 20
     * @return 当前构建器实例（链式调用）
     */
    public EsQueryBuilder page(int page, int size) {
        this.size = size < 1 ? 20 : size;
        int validPage = page < 1 ? 1 : page;
        this.from = (validPage - 1) * this.size;
        return this;
    }

    /**
     * 构建最终的 {@link NativeQuery} 对象。
     *
     * <p>将所有已配置的条件（过滤、排序、高亮、分页）组装为 Spring Data ES 可执行的查询。
     *
     * @return 构建好的 {@link NativeQuery}
     */
    public NativeQuery build() {
        Query boolQuery = Query.of(q -> q.bool(boolBuilder.build()));

        int effectiveSize = size > 0 ? size : 20;
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(from / effectiveSize, effectiveSize))
                .withMaxResults(effectiveSize);

        if (!sorts.isEmpty()) {
            queryBuilder.withSort(sorts);
        }

        if (!highlights.isEmpty()) {
            HighlightParameters parameters = HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build();
            Highlight highlight = new Highlight(parameters, highlights);
            queryBuilder.withHighlightQuery(
                    new org.springframework.data.elasticsearch.core.query.HighlightQuery(highlight, null)
            );
        }

        return queryBuilder.build();
    }
}
