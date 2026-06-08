package com.eagle.es.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EsPageResult}.
 *
 * <p>Covers field correctness, {@link EsPageResult#getTotalPages()} edge cases,
 * and various total / size combinations.
 */
@DisplayName("EsPageResult")
class EsPageResultTest {

    // -----------------------------------------------------------------------
    // Constructor and field accessors
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("fields")
    class Fields {

        @Test
        @DisplayName("应存储内容")
        void shouldStoreContent() {
            List<String> items = List.of("item1", "item2");
            EsPageResult<String> result = new EsPageResult<>(items, 100L, 1, 20);
            assertEquals(items, result.getContent());
        }

        @Test
        @DisplayName("应存储Total")
        void shouldStoreTotal() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 200L, 1, 20);
            assertEquals(200L, result.getTotal());
        }

        @Test
        @DisplayName("应存储分页")
        void shouldStorePage() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 100L, 3, 20);
            assertEquals(3, result.getPage());
        }

        @Test
        @DisplayName("应存储大小")
        void shouldStoreSize() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 100L, 1, 15);
            assertEquals(15, result.getSize());
        }
    }

    // -----------------------------------------------------------------------
    // getTotalPages()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getTotalPages()")
    class GetTotalPages {

        @Test
        @DisplayName("大小Is零时应返回零")
        void shouldReturnZeroWhenSizeIsZero() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 50L, 1, 0);
            assertEquals(0, result.getTotalPages());
        }

        @Test
        @DisplayName("TotalIs零时应返回零")
        void shouldReturnZeroWhenTotalIsZero() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 0L, 1, 10);
            assertEquals(0, result.getTotalPages());
        }

        @Test
        @DisplayName("TotalEquals大小时应返回One")
        void shouldReturnOneWhenTotalEqualsSize() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 10L, 1, 10);
            assertEquals(1, result.getTotalPages());
        }

        @Test
        @DisplayName("应轮询UpTotalPages")
        void shouldRoundUpTotalPages() {
            // 25 items / 10 per page → 3 pages (ceil(2.5) = 3)
            EsPageResult<String> result = new EsPageResult<>(List.of(), 25L, 1, 10);
            assertEquals(3, result.getTotalPages());
        }

        @Test
        @DisplayName("应返回Exact分页计数")
        void shouldReturnExactPageCount() {
            // 100 items / 20 per page → 5 pages
            EsPageResult<String> result = new EsPageResult<>(List.of(), 100L, 1, 20);
            assertEquals(5, result.getTotalPages());
        }

        @Test
        @DisplayName("TotalLessThan大小时应返回One")
        void shouldReturnOneWhenTotalLessThanSize() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 5L, 1, 20);
            assertEquals(1, result.getTotalPages());
        }

        @Test
        @DisplayName("应HandleLargeTotal")
        void shouldHandleLargeTotal() {
            // 1_000_001 / 20 → ceil(50000.05) = 50001
            EsPageResult<String> result = new EsPageResult<>(List.of(), 1_000_001L, 1, 20);
            assertEquals(50001, result.getTotalPages());
        }
    }

    // -----------------------------------------------------------------------
    // content integrity
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("content integrity")
    class ContentIntegrity {

        @Test
        @DisplayName("应支持空内容")
        void shouldSupportEmptyContent() {
            EsPageResult<String> result = new EsPageResult<>(List.of(), 0L, 1, 20);
            assertNotNull(result.getContent());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("应支持Typed内容")
        void shouldSupportTypedContent() {
            List<Integer> items = List.of(1, 2, 3);
            EsPageResult<Integer> result = new EsPageResult<>(items, 3L, 1, 20);
            assertEquals(items, result.getContent());
        }

        @Test
        @DisplayName("应保留内容Reference")
        void shouldPreserveContentReference() {
            List<String> items = List.of("a", "b");
            EsPageResult<String> result = new EsPageResult<>(items, 2L, 1, 10);
            assertSame(items, result.getContent());
        }
    }
}
