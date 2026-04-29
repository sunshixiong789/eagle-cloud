package com.eagle.es.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 通用搜索请求封装。
 *
 * <p>统一封装全文搜索所需的常用查询参数，包括关键词搜索、精确过滤、
 * 范围查询、排序和高亮等，避免在业务层直接操作 ES 原生 API。
 *
 * <p>使用示例：
 * <pre>{@code
 * EsSearchRequest request = EsSearchRequest.builder()
 *     .keyword("iPhone 15")
 *     .searchFields(List.of("title", "description"))
 *     .filters(Map.of("category", "electronics"))
 *     .sorts(Map.of("salesCount", "desc"))
 *     .highlightFields(List.of("title", "description"))
 *     .page(1)
 *     .size(20)
 *     .build();
 * }</pre>
 *
 * @author eagle
 */
@Data
@Builder
public class EsSearchRequest {

    /**
     * 全文搜索关键词，将跨 {@link #searchFields} 中的所有字段进行多字段匹配。
     * 为空时不执行全文搜索。
     */
    private String keyword;

    /**
     * 全文搜索字段列表。
     * 为空时对所有字段执行全文搜索（依赖索引配置的 _all 字段）。
     */
    @Builder.Default
    private List<String> searchFields = new ArrayList<>();

    /**
     * 精确过滤条件（字段名 → 值）。
     * 每对键值均会生成一个 term 查询（精确匹配）。
     */
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    /**
     * 范围过滤条件（字段名 → [min, max]）。
     * 数组第 0 位为 gte 下界，第 1 位为 lte 上界，{@code null} 表示不限。
     */
    @Builder.Default
    private Map<String, Object[]> rangeFilters = new HashMap<>();

    /**
     * 排序配置（字段名 → 排序方向）。
     * 排序方向使用 {@code "asc"} 或 {@code "desc"}（不区分大小写）。
     * 使用 {@link LinkedHashMap} 保证多字段排序顺序确定。
     */
    @Builder.Default
    private Map<String, String> sorts = new LinkedHashMap<>();

    /**
     * 需要高亮的字段名列表。
     * 高亮标签为 {@code <em>...</em>}，结果由 {@link com.eagle.es.util.EsHighlightUtil} 回写至文档对象。
     */
    @Builder.Default
    private List<String> highlightFields = new ArrayList<>();

    /**
     * 当前页码，从 1 开始。
     */
    @Builder.Default
    private int page = 1;

    /**
     * 每页返回的文档数量。
     */
    @Builder.Default
    private int size = 20;
}
