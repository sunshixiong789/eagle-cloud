package com.eagle.es.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.AggregationsContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EsAggregationUtil}.
 *
 * <p>{@link EsAggregationUtil} internally casts {@link AggregationsContainer} to
 * {@code ElasticsearchAggregations} (a final Spring Data ES class). Full bucket-level
 * tests require a live Elasticsearch cluster and therefore belong to integration tests.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>The utility class instantiation guard</li>
 *   <li>All null / blank input guard branches for every public method</li>
 *   <li>The wrong-type-cast branch (container is not {@code ElasticsearchAggregations})</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EsAggregationUtil")
class EsAggregationUtilTest {

    // -----------------------------------------------------------------------
    // Utility class instantiation guard
    // -----------------------------------------------------------------------

    /**
     * Minimal {@link AggregationsContainer} implementation that is intentionally
     * NOT an {@link org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations}.
     * Used to exercise the wrong-type cast branch in {@link EsAggregationUtil}.
     */
    static final class StubAggregationsContainer implements AggregationsContainer<Object> {

        @Override
        public Object aggregations() {
            return new Object();
        }
    }

    // -----------------------------------------------------------------------
    // extractTermsAgg()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("constructor guard")
    class ConstructorGuard {

        @Test
        @DisplayName("Instantiation时应抛出")
        void shouldThrowOnInstantiation() throws Exception {
            var constructor = EsAggregationUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrows(Exception.class, constructor::newInstance,
                    "Utility class must not be instantiable");
        }
    }

    // -----------------------------------------------------------------------
    // extractDateHistogramAgg()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("extractTermsAgg()")
    class ExtractTermsAgg {

        @Test
        @DisplayName("聚合null时应返回空映射")
        void shouldReturnEmptyMapWhenAggregationsNull() {
            Map<String, Long> result = EsAggregationUtil.extractTermsAgg(null, "categories");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("聚合名称null时应返回空映射")
        void shouldReturnEmptyMapWhenAggNameNull() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractTermsAgg(container, null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("聚合名称空白时应返回空映射")
        void shouldReturnEmptyMapWhenAggNameBlank() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractTermsAgg(container, "   ");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("容器Is错误类型时应返回空映射")
        void shouldReturnEmptyMapWhenContainerIsWrongType() {
            // StubAggregationsContainer is not an ElasticsearchAggregations;
            // the util will log a warning and return empty map
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractTermsAgg(container, "categories");
            assertNotNull(result);
            assertTrue(result.isEmpty(),
                    "Non-ElasticsearchAggregations container must result in empty map");
        }
    }

    // -----------------------------------------------------------------------
    // extractMetricAgg()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("extractDateHistogramAgg()")
    class ExtractDateHistogramAgg {

        @Test
        @DisplayName("聚合null时应返回空映射")
        void shouldReturnEmptyMapWhenAggregationsNull() {
            Map<String, Long> result = EsAggregationUtil.extractDateHistogramAgg(null, "daily_orders");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("聚合名称null时应返回空映射")
        void shouldReturnEmptyMapWhenAggNameNull() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractDateHistogramAgg(container, null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("聚合名称空白时应返回空映射")
        void shouldReturnEmptyMapWhenAggNameBlank() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractDateHistogramAgg(container, "  ");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("容器Is错误类型时应返回空映射")
        void shouldReturnEmptyMapWhenContainerIsWrongType() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            Map<String, Long> result = EsAggregationUtil.extractDateHistogramAgg(container, "daily_orders");
            assertNotNull(result);
            assertTrue(result.isEmpty(),
                    "Non-ElasticsearchAggregations container must result in empty map");
        }
    }

    // -----------------------------------------------------------------------
    // Stub / test helper
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("extractMetricAgg()")
    class ExtractMetricAgg {

        @Test
        @DisplayName("聚合null时应返回零")
        void shouldReturnZeroWhenAggregationsNull() {
            double result = EsAggregationUtil.extractMetricAgg(null, "total_sales");
            assertEquals(0.0, result);
        }

        @Test
        @DisplayName("聚合名称null时应返回零")
        void shouldReturnZeroWhenAggNameNull() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            double result = EsAggregationUtil.extractMetricAgg(container, null);
            assertEquals(0.0, result);
        }

        @Test
        @DisplayName("聚合名称空白时应返回零")
        void shouldReturnZeroWhenAggNameBlank() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            double result = EsAggregationUtil.extractMetricAgg(container, "   ");
            assertEquals(0.0, result);
        }

        @Test
        @DisplayName("容器Is错误类型时应返回零")
        void shouldReturnZeroWhenContainerIsWrongType() {
            AggregationsContainer<?> container = new StubAggregationsContainer();
            double result = EsAggregationUtil.extractMetricAgg(container, "total_sales");
            assertEquals(0.0, result,
                    "Non-ElasticsearchAggregations container must yield 0.0");
        }
    }
}
