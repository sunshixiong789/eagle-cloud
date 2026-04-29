package com.eagle.es.util;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.AggregationsContainer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 聚合结果提取工具类。
 *
 * <p>将 {@link AggregationsContainer} 中的原始聚合响应转换为业务友好的
 * {@link Map} 结构，避免业务层直接操作复杂的 ES 聚合 API。
 *
 * <p>目前支持以下聚合类型：
 * <ul>
 *   <li>Terms 聚合 — 如统计各商品分类的文档数</li>
 *   <li>DateHistogram 聚合 — 如按天统计订单量</li>
 *   <li>指标聚合（Sum / Avg / Max / Min）— 提取单一数值结果</li>
 * </ul>
 *
 * <p>此类为无状态工具类，所有方法均为静态方法，禁止实例化。
 *
 * @author eagle
 */
public final class EsAggregationUtil {

    private static final Logger log = LoggerFactory.getLogger(EsAggregationUtil.class);

    /**
     * 禁止实例化。
     */
    private EsAggregationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 提取 Terms 聚合结果（字符串 Terms）。
     *
     * <p>适用于商品分类统计、状态分布等按字符串字段分组统计的场景。
     * 结果按 ES 返回顺序排列（通常为文档数降序）。
     *
     * <p>使用示例：
     * <pre>{@code
     * NativeQuery query = NativeQuery.builder()
     *     .withAggregation("categories", Aggregation.of(a -> a
     *         .terms(t -> t.field("category").size(10))))
     *     .build();
     * SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);
     * Map<String, Long> stats = EsAggregationUtil.extractTermsAgg(
     *     hits.getAggregations(), "categories");
     * }</pre>
     *
     * @param aggregations 来自 {@code SearchHits#getAggregations()} 的聚合容器；可为 {@code null}
     * @param aggName      聚合名称，与查询中 {@code withAggregation} 指定的名称一致
     * @return 字段值 → 文档数的有序 {@link LinkedHashMap}；聚合不存在或解析失败时返回空 Map
     */
    public static Map<String, Long> extractTermsAgg(AggregationsContainer<?> aggregations, String aggName) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregations == null || aggName == null || aggName.isBlank()) {
            return result;
        }

        try {
            ElasticsearchAggregations esAggregations = castToElasticsearchAggregations(aggregations);
            if (esAggregations == null) {
                return result;
            }

            ElasticsearchAggregation aggregation = esAggregations.get(aggName);
            if (aggregation == null) {
                log.debug("[Eagle ES] Terms aggregation '{}' not found in response.", aggName);
                return result;
            }

            Aggregate aggregate = aggregation.aggregation().getAggregate();
            if (!aggregate.isSterms()) {
                log.debug("[Eagle ES] Aggregation '{}' is not a string terms aggregation.", aggName);
                return result;
            }

            List<StringTermsBucket> buckets = aggregate.sterms().buckets().array();
            for (StringTermsBucket bucket : buckets) {
                result.put(bucket.key().stringValue(), bucket.docCount());
            }
        } catch (Exception e) {
            log.warn("[Eagle ES] Failed to extract terms aggregation '{}': {}", aggName, e.getMessage());
        }

        return result;
    }

    /**
     * 提取 DateHistogram 聚合结果（按日期分组统计）。
     *
     * <p>适用于按天/月/年统计订单量、访问量等时序数据场景。
     * 返回的 Map 键为 ES 返回的日期字符串（格式取决于索引 mapping 或聚合配置），
     * 值为对应桶的文档数量。
     *
     * <p>使用示例：
     * <pre>{@code
     * NativeQuery query = NativeQuery.builder()
     *     .withAggregation("daily_orders", Aggregation.of(a -> a
     *         .dateHistogram(dh -> dh.field("createTime")
     *             .calendarInterval(CalendarInterval.Day))))
     *     .build();
     * Map<String, Long> daily = EsAggregationUtil.extractDateHistogramAgg(
     *     hits.getAggregations(), "daily_orders");
     * }</pre>
     *
     * @param aggregations 来自 {@code SearchHits#getAggregations()} 的聚合容器；可为 {@code null}
     * @param aggName      聚合名称
     * @return 日期字符串 → 文档数的有序 {@link LinkedHashMap}；聚合不存在或解析失败时返回空 Map
     */
    public static Map<String, Long> extractDateHistogramAgg(AggregationsContainer<?> aggregations, String aggName) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregations == null || aggName == null || aggName.isBlank()) {
            return result;
        }

        try {
            ElasticsearchAggregations esAggregations = castToElasticsearchAggregations(aggregations);
            if (esAggregations == null) {
                return result;
            }

            ElasticsearchAggregation aggregation = esAggregations.get(aggName);
            if (aggregation == null) {
                log.debug("[Eagle ES] DateHistogram aggregation '{}' not found in response.", aggName);
                return result;
            }

            Aggregate aggregate = aggregation.aggregation().getAggregate();
            if (!aggregate.isDateHistogram()) {
                log.debug("[Eagle ES] Aggregation '{}' is not a date histogram aggregation.", aggName);
                return result;
            }

            aggregate.dateHistogram().buckets().array().forEach(bucket -> {
                String dateKey = bucket.keyAsString() != null ? bucket.keyAsString() : String.valueOf(bucket.key());
                result.put(dateKey, bucket.docCount());
            });
        } catch (Exception e) {
            log.warn("[Eagle ES] Failed to extract date histogram aggregation '{}': {}", aggName, e.getMessage());
        }

        return result;
    }

    /**
     * 提取 Sum / Avg / Max / Min 等单值指标聚合结果。
     *
     * <p>支持以下 ES 聚合类型：
     * <ul>
     *   <li>{@code sum} — 字段求和</li>
     *   <li>{@code avg} — 字段求平均值</li>
     *   <li>{@code max} — 字段最大值</li>
     *   <li>{@code min} — 字段最小值</li>
     * </ul>
     *
     * <p>使用示例：
     * <pre>{@code
     * NativeQuery query = NativeQuery.builder()
     *     .withAggregation("total_sales", Aggregation.of(a -> a
     *         .sum(s -> s.field("amount"))))
     *     .build();
     * double totalSales = EsAggregationUtil.extractMetricAgg(
     *     hits.getAggregations(), "total_sales");
     * }</pre>
     *
     * @param aggregations 来自 {@code SearchHits#getAggregations()} 的聚合容器；可为 {@code null}
     * @param aggName      聚合名称
     * @return 聚合计算结果；聚合不存在、值为 null 或解析失败时返回 {@code 0.0}
     */
    public static double extractMetricAgg(AggregationsContainer<?> aggregations, String aggName) {
        if (aggregations == null || aggName == null || aggName.isBlank()) {
            return 0.0;
        }

        try {
            ElasticsearchAggregations esAggregations = castToElasticsearchAggregations(aggregations);
            if (esAggregations == null) {
                return 0.0;
            }

            ElasticsearchAggregation aggregation = esAggregations.get(aggName);
            if (aggregation == null) {
                log.debug("[Eagle ES] Metric aggregation '{}' not found in response.", aggName);
                return 0.0;
            }

            Aggregate aggregate = aggregation.aggregation().getAggregate();

            if (aggregate.isSum()) {
                Double value = aggregate.sum().value();
                return value != null ? value : 0.0;
            } else if (aggregate.isAvg()) {
                Double value = aggregate.avg().value();
                return value != null ? value : 0.0;
            } else if (aggregate.isMax()) {
                Double value = aggregate.max().value();
                return value != null ? value : 0.0;
            } else if (aggregate.isMin()) {
                Double value = aggregate.min().value();
                return value != null ? value : 0.0;
            } else {
                log.debug("[Eagle ES] Aggregation '{}' is not a supported metric aggregation type.", aggName);
                return 0.0;
            }
        } catch (Exception e) {
            log.warn("[Eagle ES] Failed to extract metric aggregation '{}': {}", aggName, e.getMessage());
            return 0.0;
        }
    }

    /**
     * 将 {@link AggregationsContainer} 安全转换为 {@link ElasticsearchAggregations}。
     *
     * <p>Spring Data ES 使用 {@link ElasticsearchAggregations} 作为 ELC（Elasticsearch Java Client）
     * 的聚合容器实现，转换失败时记录警告日志并返回 {@code null}。
     *
     * @param aggregations 待转换的聚合容器
     * @return 转换后的 {@link ElasticsearchAggregations}，类型不匹配时返回 {@code null}
     */
    private static ElasticsearchAggregations castToElasticsearchAggregations(AggregationsContainer<?> aggregations) {
        if (aggregations instanceof ElasticsearchAggregations esAggregations) {
            return esAggregations;
        }
        log.warn("[Eagle ES] AggregationsContainer is not an ElasticsearchAggregations instance: {}",
                aggregations.getClass().getName());
        return null;
    }
}
