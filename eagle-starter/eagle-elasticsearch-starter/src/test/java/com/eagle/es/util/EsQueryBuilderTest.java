package com.eagle.es.util;

import co.elastic.clients.elasticsearch._types.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import static org.junit.jupiter.api.Assertions.*;

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
        @DisplayName("should return a non-null builder instance")
        void shouldReturnNonNullInstance() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("should return distinct instances on each call")
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
        @DisplayName("should return non-null NativeQuery for empty builder")
        void shouldReturnNonNullForEmptyBuilder() {
            NativeQuery query = EsQueryBuilder.of().build();
            assertNotNull(query);
            assertNotNull(query.getQuery(), "Query object must not be null");
        }

        @Test
        @DisplayName("should return non-null NativeQuery after multiMatch")
        void shouldReturnNativeQueryForMultiMatch() {
            NativeQuery query = EsQueryBuilder.of()
                    .multiMatch("手机", "name", "description")
                    .build();

            assertNotNull(query);
            assertNotNull(query.getQuery());
        }

        @Test
        @DisplayName("should apply pagination: from = (page-1)*size, size = configured size")
        void shouldApplyPagination() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(2, 10)
                    .build();

            assertNotNull(query);
            assertEquals(10, query.getFrom(), "from should be (2-1)*10 = 10");
            assertEquals(10, query.getMaxResults(), "maxResults should equal the configured page size");
        }

        @Test
        @DisplayName("should apply default pagination: from=0, size=20 when page() is not called")
        void shouldApplyDefaultPagination() {
            NativeQuery query = EsQueryBuilder.of().build();

            assertNotNull(query);
            assertEquals(0, query.getFrom(), "default from should be 0");
            assertEquals(20, query.getMaxResults(), "default size should be 20");
        }

        @Test
        @DisplayName("should apply first page correctly: from=0 for page(1, 10)")
        void shouldApplyFirstPage() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(1, 10)
                    .build();

            assertEquals(0, query.getFrom(), "page 1 should have from=0");
            assertEquals(10, query.getMaxResults());
        }

        @Test
        @DisplayName("should reset page<1 to first page")
        void shouldResetInvalidPageToFirst() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(0, 10)
                    .build();

            assertEquals(0, query.getFrom(), "page 0 should be treated as page 1, from=0");
        }

        @Test
        @DisplayName("should reset size<1 to default 20")
        void shouldResetInvalidSizeToDefault() {
            NativeQuery query = EsQueryBuilder.of()
                    .page(1, 0)
                    .build();

            assertEquals(20, query.getMaxResults(), "size 0 should fall back to 20");
        }

        @Test
        @DisplayName("should not throw when chaining multiMatch + term + range + page")
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
        @DisplayName("should not throw when chaining terms + prefix + wildcard + sort + highlight + page")
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
        @DisplayName("should include highlight query when highlight fields are configured")
        void shouldIncludeHighlightWhenConfigured() {
            NativeQuery query = EsQueryBuilder.of()
                    .highlight("name", "description")
                    .build();

            assertNotNull(query);
            assertNotNull(query.getHighlightQuery(), "HighlightQuery should be present when fields are set");
        }

        @Test
        @DisplayName("should not include highlight query when no highlight fields are configured")
        void shouldNotIncludeHighlightWhenNotConfigured() {
            NativeQuery query = EsQueryBuilder.of().build();

            assertNull(query.getHighlightQuery(), "HighlightQuery should be null when no highlight fields");
        }

        @Test
        @DisplayName("should include sort options when sort() is called")
        void shouldIncludeSortWhenConfigured() {
            NativeQuery query = EsQueryBuilder.of()
                    .sort("price", SortOrder.Desc)
                    .build();

            assertNotNull(query);
            assertNotNull(query.getSortOptions());
            assertFalse(query.getSortOptions().isEmpty(), "SortOptions should be non-empty");
        }

        @Test
        @DisplayName("should have empty sort options when sort() is never called")
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
        @DisplayName("should skip condition when keyword is null")
        void shouldSkipWhenKeywordIsNull() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch(null, "title")
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("should skip condition when keyword is blank")
        void shouldSkipWhenKeywordIsBlank() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .multiMatch("   ", "title")
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("should return this for chaining when keyword is blank")
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
        @DisplayName("should skip condition when field is null")
        void shouldSkipWhenFieldIsNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().term(null, "value").build());
        }

        @Test
        @DisplayName("should skip condition when value is null")
        void shouldSkipWhenValueIsNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().term("field", null).build());
        }

        @Test
        @DisplayName("should support any Object value via toString()")
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
        @DisplayName("should skip condition when values array is empty")
        void shouldSkipWhenValuesEmpty() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().terms("field").build());
        }

        @Test
        @DisplayName("should skip condition when field is blank")
        void shouldSkipWhenFieldIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().terms("  ", "a", "b").build());
        }

        @Test
        @DisplayName("should filter out null values in the array")
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
        @DisplayName("should skip condition when both min and max are null")
        void shouldSkipWhenBothNull() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().range("price", null, null).build());
        }

        @Test
        @DisplayName("should allow only min (open-ended upper bound)")
        void shouldAllowOnlyMin() {
            assertDoesNotThrow(() -> {
                NativeQuery query = EsQueryBuilder.of()
                        .range("price", 100, null)
                        .build();
                assertNotNull(query);
            });
        }

        @Test
        @DisplayName("should allow only max (open-ended lower bound)")
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
        @DisplayName("should skip condition when field is blank")
        void shouldSkipWhenFieldIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().prefix("", "SKU-").build());
        }

        @Test
        @DisplayName("should skip condition when prefix is blank")
        void shouldSkipWhenPrefixIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().prefix("sku", "").build());
        }

        @Test
        @DisplayName("should add prefix condition when valid")
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
        @DisplayName("should skip condition when pattern is blank")
        void shouldSkipWhenPatternIsBlank() {
            assertDoesNotThrow(() -> EsQueryBuilder.of().wildcard("name", "  ").build());
        }

        @Test
        @DisplayName("should add wildcard condition when valid")
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
        @DisplayName("should skip when field is blank")
        void shouldSkipWhenFieldIsBlank() {
            NativeQuery query = EsQueryBuilder.of().sort("", SortOrder.Asc).build();
            assertTrue(query.getSortOptions() == null || query.getSortOptions().isEmpty());
        }

        @Test
        @DisplayName("should skip when order is null")
        void shouldSkipWhenOrderIsNull() {
            NativeQuery query = EsQueryBuilder.of().sort("price", null).build();
            assertTrue(query.getSortOptions() == null || query.getSortOptions().isEmpty());
        }

        @Test
        @DisplayName("should accumulate multiple sorts in order")
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
        @DisplayName("should skip when fields array is empty")
        void shouldSkipWhenFieldsEmpty() {
            NativeQuery query = EsQueryBuilder.of().highlight().build();
            assertNull(query.getHighlightQuery(), "No highlight query when no fields are provided");
        }

        @Test
        @DisplayName("should skip blank field names within array")
        void shouldSkipBlankFieldNames() {
            // Only "title" is valid; blank strings are filtered out
            NativeQuery query = EsQueryBuilder.of()
                    .highlight("title", "  ", "")
                    .build();
            // Highlight query should still be present because "title" is valid
            assertNotNull(query.getHighlightQuery());
        }

        @Test
        @DisplayName("should be chainable and return this")
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
        @DisplayName("should compute correct from for page 3 with size 15")
        void shouldComputeCorrectFrom() {
            NativeQuery query = EsQueryBuilder.of().page(3, 15).build();
            assertEquals(30, query.getFrom(), "from = (3-1)*15 = 30");
            assertEquals(15, query.getMaxResults());
        }

        @Test
        @DisplayName("should be chainable and return this")
        void shouldBeChainable() {
            EsQueryBuilder builder = EsQueryBuilder.of();
            EsQueryBuilder result = builder.page(1, 10);
            assertSame(builder, result);
        }
    }
}
