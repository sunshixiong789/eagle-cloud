package com.eagle.es.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        @DisplayName("should have page=1 by default")
        void shouldHaveDefaultPage() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertEquals(1, request.getPage());
        }

        @Test
        @DisplayName("should have size=20 by default")
        void shouldHaveDefaultSize() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertEquals(20, request.getSize());
        }

        @Test
        @DisplayName("should have empty searchFields list by default")
        void shouldHaveEmptySearchFieldsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getSearchFields());
            assertTrue(request.getSearchFields().isEmpty());
        }

        @Test
        @DisplayName("should have empty filters map by default")
        void shouldHaveEmptyFiltersByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getFilters());
            assertTrue(request.getFilters().isEmpty());
        }

        @Test
        @DisplayName("should have empty rangeFilters map by default")
        void shouldHaveEmptyRangeFiltersByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getRangeFilters());
            assertTrue(request.getRangeFilters().isEmpty());
        }

        @Test
        @DisplayName("should have empty sorts map by default")
        void shouldHaveEmptySortsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getSorts());
            assertTrue(request.getSorts().isEmpty());
        }

        @Test
        @DisplayName("should have empty highlightFields list by default")
        void shouldHaveEmptyHighlightFieldsByDefault() {
            EsSearchRequest request = EsSearchRequest.builder().build();
            assertNotNull(request.getHighlightFields());
            assertTrue(request.getHighlightFields().isEmpty());
        }

        @Test
        @DisplayName("keyword should be null when not set")
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
        @DisplayName("should store keyword correctly")
        void shouldSetKeyword() {
            EsSearchRequest request = EsSearchRequest.builder()
                    .keyword("iPhone 15")
                    .build();
            assertEquals("iPhone 15", request.getKeyword());
        }

        @Test
        @DisplayName("should store searchFields correctly")
        void shouldSetSearchFields() {
            List<String> fields = List.of("title", "description");
            EsSearchRequest request = EsSearchRequest.builder()
                    .searchFields(fields)
                    .build();
            assertEquals(fields, request.getSearchFields());
        }

        @Test
        @DisplayName("should store both keyword and searchFields together")
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
        @DisplayName("should store filters map correctly")
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
        @DisplayName("should store rangeFilters map correctly")
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
        @DisplayName("should store sorts map correctly")
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
        @DisplayName("should store highlightFields correctly")
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
        @DisplayName("should store custom page and size correctly")
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
        @DisplayName("should correctly populate all fields when all builder methods are called")
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
