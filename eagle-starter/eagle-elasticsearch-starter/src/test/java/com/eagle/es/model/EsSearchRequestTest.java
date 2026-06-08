package com.eagle.es.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EsSearchRequest}.
 *
 * <p>Verifies builder-created instances carry the correct field values,
 * and that {@code @Builder.Default} annotations produce the expected
 * defaults on fields that are not explicitly set.
 */
@DisplayName("EsSearchRequest")
class EsSearchRequestTest {

    // -----------------------------------------------------------------------
    // Default values
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("default values")
    class DefaultValues {

        @Test
        @DisplayName("应Have默认分页")
        void shouldHaveDefaultPage() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertEquals(1, request.getPage());
        }

        @Test
        @DisplayName("应Have默认大小")
        void shouldHaveDefaultSize() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertEquals(20, request.getSize());
        }

        @Test
        @DisplayName("应Have空搜索字段通过默认")
        void shouldHaveEmptySearchFieldsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getSearchFields());
            assertTrue(request.getSearchFields().isEmpty());
        }

        @Test
        @DisplayName("应Have空Filters通过默认")
        void shouldHaveEmptyFiltersByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getFilters());
            assertTrue(request.getFilters().isEmpty());
        }

        @Test
        @DisplayName("应Have空RangeFilters通过默认")
        void shouldHaveEmptyRangeFiltersByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getRangeFilters());
            assertTrue(request.getRangeFilters().isEmpty());
        }

        @Test
        @DisplayName("应Have空Sorts通过默认")
        void shouldHaveEmptySortsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getSorts());
            assertTrue(request.getSorts().isEmpty());
        }

        @Test
        @DisplayName("应Have空高亮字段通过默认")
        void shouldHaveEmptyHighlightFieldsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getHighlightFields());
            assertTrue(request.getHighlightFields().isEmpty());
        }

        @Test
        @DisplayName("应Havenull关键字通过默认")
        void shouldHaveNullKeywordByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNull(request.getKeyword());
        }
    }

    // -----------------------------------------------------------------------
    // keyword and searchFields
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("keyword and searchFields")
    class KeywordAndSearchFields {

        @Test
        @DisplayName("应设置关键字")
        void shouldSetKeyword() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .keyword("iPhone 15")
                    .build();
            assertEquals("iPhone 15", request.getKeyword());
        }

        @Test
        @DisplayName("应设置搜索字段")
        void shouldSetSearchFields() {
            List<String> fields = List.of("title", "description");
            EsSearchRequest request = EsSearchRequest.builder()
                    .searchFields(fields)
                    .build();
            assertEquals(fields, request.getSearchFields());
        }

        @Test
        @DisplayName("应设置关键字并搜索字段Together")
        void shouldSetKeywordAndSearchFieldsTogether() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .keyword("手机")
                    .searchFields(List.of("name", "description"))
                    .build();
            assertEquals("手机", request.getKeyword());
            assertEquals(List.of("name", "description"), request.getSearchFields());
        }
    }

    // -----------------------------------------------------------------------
    // filters
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("filters")
    class Filters {

        @Test
        @DisplayName("使用Filters时应构建")
        void shouldBuildWithFilters() {
            Map<String, Object> filters = Map.of("category", "electronics", "brand", "Apple");
            EsSearchRequest request = EsSearchRequest.builder()
                    .filters(filters)
                    .build();
            assertEquals(filters, request.getFilters());
            assertEquals("electronics", request.getFilters().get("category"));
            assertEquals("Apple", request.getFilters().get("brand"));
        }

        @Test
        @DisplayName("使用RangeFilters时应构建")
        void shouldBuildWithRangeFilters() {
            Object[] priceRange = {100, 5000};
            Map<String, Object[]> rangeFilters = Map.of("price", priceRange);
            EsSearchRequest request = EsSearchRequest.builder()
                    .rangeFilters(rangeFilters)
                    .build();
            assertArrayEquals(priceRange, request.getRangeFilters().get("price"));
        }
    }

    // -----------------------------------------------------------------------
    // sorts
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("sorts")
    class Sorts {

        @Test
        @DisplayName("应存储Sorts")
        void shouldStoreSorts() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .sorts(Map.of("salesCount", "desc", "price", "asc"))
                    .build();
            assertEquals("desc", request.getSorts().get("salesCount"));
            assertEquals("asc", request.getSorts().get("price"));
        }
    }

    // -----------------------------------------------------------------------
    // highlightFields
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("highlightFields")
    class HighlightFields {

        @Test
        @DisplayName("应存储高亮字段")
        void shouldStoreHighlightFields() {
            List<String> fields = List.of("title", "description");
            EsSearchRequest request = EsSearchRequest.builder()
                    .highlightFields(fields)
                    .build();
            assertEquals(fields, request.getHighlightFields());
        }
    }

    // -----------------------------------------------------------------------
    // pagination
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("应存储自定义分页并大小")
        void shouldStoreCustomPageAndSize() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .page(3)
                    .size(50)
                    .build();
            assertEquals(3, request.getPage());
            assertEquals(50, request.getSize());
        }
    }

    // -----------------------------------------------------------------------
    // full builder scenario
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("full builder scenario")
    class FullBuilder {

        @Test
        @DisplayName("应Populate全部字段")
        void shouldPopulateAllFields() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .keyword("iPhone 15")
                    .searchFields(List.of("title", "description"))
                    .filters(Map.of("category", "electronics"))
                    .rangeFilters(Map.of("price", new Object[]{100, 5000}))
                    .sorts(Map.of("salesCount", "desc"))
                    .highlightFields(List.of("title"))
                    .page(2)
                    .size(10)
                    .build();

            assertEquals("iPhone 15", request.getKeyword());
            assertEquals(List.of("title", "description"), request.getSearchFields());
            assertEquals("electronics", request.getFilters().get("category"));
            assertNotNull(request.getRangeFilters().get("price"));
            assertEquals("desc", request.getSorts().get("salesCount"));
            assertEquals(List.of("title"), request.getHighlightFields());
            assertEquals(2, request.getPage());
            assertEquals(10, request.getSize());
        }
    }
}
