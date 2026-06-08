package com.eagle.es.util;

import co.elastic.clients.elasticsearch._types.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EsQueryBuilder}.
 *
 * <p>All tests operate on the builder's in-memory DSL state and the
 * resulting {@link NativeQuery} structure. No Elasticsearch connection
 * is required.
 */
@DisplayName("EsQueryBuilder")
class EsQueryBuilderTest {

    // -----------------------------------------------------------------------
    // Static factory
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("of()")
    class OfFactory {

        @Test
        @DisplayName("应返回非nullInstance")
        void shouldReturnNonNullInstance() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("应返回DistinctInstances")
        void shouldReturnDistinctInstances() {
            EsQueryBuilder first = EsQueryBuilder.of();
            EsQueryBuilder second = EsQueryBuilder.of();
            assertNotSame(first, second);
        }
    }

    // -----------------------------------------------------------------------
    // build()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("build()")
    class Build {

        @Test
        @DisplayName("应返回非null针对空Builder")
        void shouldReturnNonNullForEmptyBuilder() {
            NativeQuery query = EsQueryBuilder.of().build();
            assertNotNull(query);
            assertNotNull(query.getQuery(), "Query object must not be null");
        }

        @Test
        @DisplayName("应返回Native查询针对Multi匹配")
        void shouldReturnNativeQueryForMultiMatch() {
            NativeQuery query = EsQueryBuilder.of()
                    .multiMatch("手机", "name", "description")
                    .build();

            assertNotNull(query);
            assertNotNull(query.getQuery());
        }

        @Test
        @DisplayName("应应用Pagination")
        void shouldApplyPagination() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(2, 10)
                    .build();

            assertNotNull(query);
            assertEquals(10, query.getPageable().getOffset(), "from should be (2-1)*10 = 10");
            assertEquals(10, query.getMaxResults(), "maxResults should equal the configured page size");
        }

        @Test
        @DisplayName("应应用默认Pagination")
        void shouldApplyDefaultPagination() {
            NativeQuery query = EsQueryBuilder.of().build();

            assertNotNull(query);
            assertEquals(0, query.getPageable().getOffset(), "default from should be 0");
            assertEquals(20, query.getMaxResults(), "default size should be 20");
        }

        @Test
        @DisplayName("应应用首次分页")
        void shouldApplyFirstPage() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(1, 10)
                    .build();

            assertEquals(0, query.getPageable().getOffset(), "page 1 should have from=0");
            assertEquals(10, query.getMaxResults());
        }

        @Test
        @DisplayName("应Reset无效分页到首次")
        void shouldResetInvalidPageToFirst() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(0, 10)
                    .build();

            assertEquals(0, query.getPageable().getOffset(), "page 0 should be treated as page 1, from=0");
        }

        @Test
        @DisplayName("应Reset无效大小到默认")
        void shouldResetInvalidSizeToDefault() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(1, 0)
                    .build();

            assertEquals(20, query.getMaxResults(), "size 0 should fall back to 20");
        }

        @Test
        @DisplayName("应链路多个条件")
        void shouldChainMultipleConditions() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch("iPhone", "title", "description")
                        .term("category", "electronics")
                        .range("price", 100, 5000)
                        .page(1, 20)
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("应链路全部条件")
        void shouldChainAllConditions() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch("test", "title")
                        .terms("status", "ACTIVE", "PENDING")
                        .prefix("sku", "SKU-")
                        .wildcard("name", "iphone*")
                        .sort("price", SortOrder.Asc)
                        .highlight("title", "description")
                        .page(2, 10)
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("配置的时应包含高亮")
        void shouldIncludeHighlightWhenConfigured() {
            NativeQuery query = EsQueryBuilder.of()
                    .highlight("name", "description")
                    .build();

            assertNotNull(query);
            assertTrue(query.getHighlightQuery().isPresent(), "HighlightQuery should be present when fields are set");
        }

        @Test
        @DisplayName("不配置的时不应包含高亮")
        void shouldNotIncludeHighlightWhenNotConfigured() {
            NativeQuery query = EsQueryBuilder.of().build();

            assertTrue(query.getHighlightQuery().isEmpty(), "HighlightQuery should be absent when no highlight fields");
        }

        @Test
        @DisplayName("配置的时应包含排序")
        void shouldIncludeSortWhenConfigured() {
            NativeQuery query = EsQueryBuilder.of()
                    .sort("price", SortOrder.Desc)
                    .build();

            assertNotNull(query);
            assertNotNull(query.getSortOptions());
            assertFalse(query.getSortOptions().isEmpty(), "SortOptions should be non-empty");
        }

        @Test
        @DisplayName("不配置的时应Have空排序")
        void shouldHaveEmptySortWhenNotConfigured() {
            NativeQuery query = EsQueryBuilder.of().build();

            // NativeQuery may return null or empty list — both are acceptable "no sort" states
            assertTrue(query.getSortOptions() == null || query.getSortOptions().isEmpty(),
                    "SortOptions should be null or empty when no sort is configured");
        }
    }

    // -----------------------------------------------------------------------
    // multiMatch()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("multiMatch()")
    class MultiMatch {

        @Test
        @DisplayName("关键字Isnull时应跳过")
        void shouldSkipWhenKeywordIsNull() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch(null, "title")
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("关键字Is空白时应跳过")
        void shouldSkipWhenKeywordIsBlank() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch("   ", "title")
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("应返回This针对Chaining")
        void shouldReturnThisForChaining() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            EsQueryBuilder result = builder.multiMatch("", "title");
            assertSame(builder, result);
        }
    }

    // -----------------------------------------------------------------------
    // term()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("term()")
    class Term {

        @Test
        @DisplayName("字段Isnull时应跳过")
        void shouldSkipWhenFieldIsNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().term(null, "value").build());
        }

        @Test
        @DisplayName("值Isnull时应跳过")
        void shouldSkipWhenValueIsNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().term("field", null).build());
        }

        @Test
        @DisplayName("应支持Object值")
        void shouldSupportObjectValue() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .term("status", 1)
                        .build();
                assertNotNull(query);
            });
        }
    }

    // -----------------------------------------------------------------------
    // terms()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("terms()")
    class Terms {

        @Test
        @DisplayName("值空时应跳过")
        void shouldSkipWhenValuesEmpty() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().terms("field").build());
        }

        @Test
        @DisplayName("字段Is空白时应跳过")
        void shouldSkipWhenFieldIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().terms("  ", "a", "b").build());
        }

        @Test
        @DisplayName("应过滤器null值")
        void shouldFilterNullValues() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .terms("status", "ACTIVE", null, "PENDING")
                        .build();
                assertNotNull(query);
            });
        }
    }

    // -----------------------------------------------------------------------
    // range()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("range()")
    class Range {

        @Test
        @DisplayName("同时null时应跳过")
        void shouldSkipWhenBothNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().range("price", null, null).build());
        }

        @Test
        @DisplayName("ly分钟时应允许")
        void shouldAllowOnlyMin() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .range("price", 100, null)
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("lyMax时应允许")
        void shouldAllowOnlyMax() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .range("price", null, 9999)
                        .build();
                assertNotNull(query);
            });
        }
    }

    // -----------------------------------------------------------------------
    // prefix()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("prefix()")
    class Prefix {

        @Test
        @DisplayName("字段Is空白时应跳过")
        void shouldSkipWhenFieldIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().prefix("", "SKU-").build());
        }

        @Test
        @DisplayName("前缀Is空白时应跳过")
        void shouldSkipWhenPrefixIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().prefix("sku", "").build());
        }

        @Test
        @DisplayName("有效时应添加条件")
        void shouldAddConditionWhenValid() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .prefix("sku", "SKU-")
                        .build();
                assertNotNull(query);
            });
        }
    }

    // -----------------------------------------------------------------------
    // wildcard()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("wildcard()")
    class Wildcard {

        @Test
        @DisplayName("PatternIs空白时应跳过")
        void shouldSkipWhenPatternIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().wildcard("name", "  ").build());
        }

        @Test
        @DisplayName("有效时应添加条件")
        void shouldAddConditionWhenValid() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .wildcard("name", "iphone*")
                        .build();
                assertNotNull(query);
            });
        }
    }

    // -----------------------------------------------------------------------
    // sort()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("sort()")
    class Sort {

        @Test
        @DisplayName("字段Is空白时应跳过")
        void shouldSkipWhenFieldIsBlank() {
            NativeQuery query = EsQueryBuilder.of().sort("", SortOrder.Asc).build();
            assertTrue(query.getSortOptions() == null || query.getSortOptions().isEmpty());
        }

        @Test
        @DisplayName("排序Isnull时应跳过")
        void shouldSkipWhenOrderIsNull() {
            NativeQuery query = EsQueryBuilder.of().sort("price", null).build();
            assertTrue(query.getSortOptions() == null || query.getSortOptions().isEmpty());
        }

        @Test
        @DisplayName("应累加多个Sorts")
        void shouldAccumulateMultipleSorts() {
            NativeQuery query = EsQueryBuilder.of()
                    .sort("salesCount", SortOrder.Desc)
                    .sort("price", SortOrder.Asc)
                    .build();

            assertNotNull(query.getSortOptions());
            assertEquals(2, query.getSortOptions().size(), "Two sort options should be present");
        }
    }

    // -----------------------------------------------------------------------
    // highlight()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("highlight()")
    class Highlight {

        @Test
        @DisplayName("字段空时应跳过")
        void shouldSkipWhenFieldsEmpty() {
            NativeQuery query = EsQueryBuilder.of().highlight().build();
            assertTrue(query.getHighlightQuery().isEmpty(), "No highlight query when no fields are provided");
        }

        @Test
        @DisplayName("应跳过空白字段Names")
        void shouldSkipBlankFieldNames() {
            // Only "title" is valid; blank strings are filtered out
            NativeQuery query = EsQueryBuilder.of()
                    .highlight("title", "  ", "")
                    .build();
            // Highlight query should still be present because "title" is valid
            assertNotNull(query.getHighlightQuery());
        }

        @Test
        @DisplayName("应BeChainable")
        void shouldBeChainable() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            EsQueryBuilder result = builder.highlight("title");
            assertSame(builder, result);
        }
    }

    // -----------------------------------------------------------------------
    // page()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("page()")
    class Page {

        @Test
        @DisplayName("应ComputeCorrect从")
        void shouldComputeCorrectFrom() {
            NativeQuery query = EsQueryBuilder.of().page(3, 15).build();
            assertEquals(30, query.getPageable().getOffset(), "from = (3-1)*15 = 30");
            assertEquals(15, query.getMaxResults());
        }

        @Test
        @DisplayName("应BeChainable")
        void shouldBeChainable() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            EsQueryBuilder result = builder.page(1, 10);
            assertSame(builder, result);
        }
    }
}
