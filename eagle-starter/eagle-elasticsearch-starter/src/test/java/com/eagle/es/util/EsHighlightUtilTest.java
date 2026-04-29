package com.eagle.es.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EsHighlightUtil}.
 *
 * <p>Uses Mockito to supply {@link SearchHit} objects without connecting to
 * a real Elasticsearch cluster.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EsHighlightUtil")
class EsHighlightUtilTest {

    // -----------------------------------------------------------------------
    // Utility class instantiation guard
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("constructor guard")
    class ConstructorGuard {

        @Test
        @DisplayName("should throw UnsupportedOperationException when instantiated via reflection")
        void shouldThrowOnInstantiation() throws Exception {
            var constructor = EsHighlightUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrows(Exception.class, constructor::newInstance,
                    "Utility class must not be instantiable");
        }
    }

    // -----------------------------------------------------------------------
    // applyHighlight() — null / empty guard cases
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("applyHighlight() — guard cases")
    class ApplyHighlightGuards {

        @Test
        @DisplayName("should do nothing when hit is null")
        void shouldDoNothingWhenHitIsNull() {
            assertDoesNotThrow(() -> EsHighlightUtil.applyHighlight(null));
        }

        @Test
        @DisplayName("should do nothing when hit content is null")
        @SuppressWarnings("unchecked")
        void shouldDoNothingWhenContentIsNull() {
            SearchHit<Object> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(null);

            assertDoesNotThrow(() -> EsHighlightUtil.applyHighlight(hit));
        }

        @Test
        @DisplayName("should do nothing when highlight fields map is empty")
        @SuppressWarnings("unchecked")
        void shouldDoNothingWhenHighlightFieldsEmpty() {
            TestDocument doc = new TestDocument("original title");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(Map.of());

            EsHighlightUtil.applyHighlight(hit);

            assertEquals("original title", doc.getTitle(),
                    "Content must not be modified when there are no highlight fields");
        }

        @Test
        @DisplayName("should do nothing when highlight fields map is null")
        @SuppressWarnings("unchecked")
        void shouldDoNothingWhenHighlightFieldsNull() {
            TestDocument doc = new TestDocument("original title");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(null);

            EsHighlightUtil.applyHighlight(hit);

            assertEquals("original title", doc.getTitle());
        }
    }

    // -----------------------------------------------------------------------
    // applyHighlight() — successful replacement
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("applyHighlight() — highlight replacement")
    class ApplyHighlightReplacement {

        @Test
        @DisplayName("should replace field value with single highlight fragment")
        @SuppressWarnings("unchecked")
        void shouldReplaceSingleFragment() {
            TestDocument doc = new TestDocument("iPhone 15 Pro");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(
                    Map.of("title", List.of("<em>iPhone</em> 15 Pro"))
            );

            EsHighlightUtil.applyHighlight(hit);

            assertEquals("<em>iPhone</em> 15 Pro", doc.getTitle(),
                    "Field must be replaced with the highlighted fragment");
        }

        @Test
        @DisplayName("should join multiple fragments with '...' separator")
        @SuppressWarnings("unchecked")
        void shouldJoinMultipleFragments() {
            TestDocument doc = new TestDocument("Long description about iPhone 15 Pro Max");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(
                    Map.of("title", List.of("<em>iPhone</em> 15", "Pro <em>Max</em>"))
            );

            EsHighlightUtil.applyHighlight(hit);

            assertEquals("<em>iPhone</em> 15...<em>Pro Max</em>", doc.getTitle(),
                    "Multiple fragments should be joined with '...'");
        }

        @Test
        @DisplayName("should skip field when fragment list is empty")
        @SuppressWarnings("unchecked")
        void shouldSkipWhenFragmentListEmpty() {
            TestDocument doc = new TestDocument("original");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(
                    Map.of("title", List.of())
            );

            EsHighlightUtil.applyHighlight(hit);

            assertEquals("original", doc.getTitle(),
                    "Field must not be modified when fragment list is empty");
        }

        @Test
        @DisplayName("should skip field when the field does not exist in the document class")
        @SuppressWarnings("unchecked")
        void shouldSkipWhenFieldNotFoundInDocument() {
            TestDocument doc = new TestDocument("original");
            SearchHit<TestDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(
                    Map.of("nonExistentField", List.of("<em>something</em>"))
            );

            // Should not throw; should simply ignore the unknown field
            assertDoesNotThrow(() -> EsHighlightUtil.applyHighlight(hit));
            assertEquals("original", doc.getTitle());
        }

        @Test
        @DisplayName("should skip non-String fields (only String fields are replaceable)")
        @SuppressWarnings("unchecked")
        void shouldSkipNonStringFields() {
            DocumentWithIntField doc = new DocumentWithIntField(42);
            SearchHit<DocumentWithIntField> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            when(hit.getHighlightFields()).thenReturn(
                    Map.of("count", List.of("<em>42</em>"))
            );

            // Must not throw; non-String fields should be silently skipped
            assertDoesNotThrow(() -> EsHighlightUtil.applyHighlight(hit));
            assertEquals(42, doc.getCount(), "Integer field must not be modified");
        }
    }

    // -----------------------------------------------------------------------
    // Helper document classes for testing
    // -----------------------------------------------------------------------

    /** Minimal document used as a reflection target for highlight application. */
    static class TestDocument {
        private String title;

        TestDocument(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }
    }

    /** Document with a non-String field to verify the type-check guard. */
    static class DocumentWithIntField {
        private int count;

        DocumentWithIntField(int count) {
            this.count = count;
        }

        int getCount() {
            return count;
        }
    }
}
