package com.eagle.common.pressuretest;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PressureTestFilter}.
 *
 * <p>Calls {@code doFilterInternal} directly to avoid Spring MVC context setup.
 */
@ExtendWith(MockitoExtension.class)
class PressureTestFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private PressureTestFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PressureTestFilter();
        // OncePerRequestFilter.doFilter checks getAttribute to detect re-entry — null means first pass.
        // It also checks getDispatcherType to detect async dispatching.
        lenient().when(request.getAttribute(any(String.class))).thenReturn(null);
        lenient().when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    }

    @AfterEach
    void tearDown() {
        PressureTestContext.clear();
    }

    /**
     * Invokes the protected {@code doFilterInternal} method directly via reflection-free subclass trick.
     */
    private void invokeFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // PressureTestFilter extends OncePerRequestFilter; doFilter -> doFilterInternal.
        // We trigger it by calling doFilter on a mock request that has not been filtered yet.
        filter.doFilter(req, res, chain);
    }

    @Nested
    @DisplayName("doFilterInternal — header present")
    class HeaderPresent {

        @Test
        @DisplayName("should mark PressureTestContext when X-Eagle-Gray header is 'true'")
        void doFilter_shouldMarkContextWhenHeaderPresent() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn("true");
            AtomicBoolean markedDuringFilter = new AtomicBoolean(false);

            doAnswer(invocation -> {
                // Capture the flag state *inside* the filter chain execution
                markedDuringFilter.set(PressureTestContext.isPressureTest());
                return null;
            }).when(filterChain).doFilter(any(), any());

            invokeFilter(request, response, filterChain);

            assertTrue(markedDuringFilter.get(),
                    "PressureTestContext should be marked true while executing the filter chain");
        }

        @Test
        @DisplayName("should mark context for case-insensitive 'TRUE' header value")
        void doFilter_shouldMarkContextForCaseInsensitiveTrue() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn("TRUE");
            AtomicBoolean markedDuringFilter = new AtomicBoolean(false);

            doAnswer(invocation -> {
                markedDuringFilter.set(PressureTestContext.isPressureTest());
                return null;
            }).when(filterChain).doFilter(any(), any());

            invokeFilter(request, response, filterChain);

            assertTrue(markedDuringFilter.get());
        }
    }

    @Nested
    @DisplayName("doFilterInternal — header absent")
    class HeaderAbsent {

        @Test
        @DisplayName("should not mark PressureTestContext when header is absent")
        void doFilter_shouldNotMarkContextWhenHeaderAbsent() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn(null);
            AtomicBoolean markedDuringFilter = new AtomicBoolean(true);

            doAnswer(invocation -> {
                markedDuringFilter.set(PressureTestContext.isPressureTest());
                return null;
            }).when(filterChain).doFilter(any(), any());

            invokeFilter(request, response, filterChain);

            assertFalse(markedDuringFilter.get());
        }
    }

    @Nested
    @DisplayName("doFilterInternal — finally block clears context")
    class FinallyClears {

        @Test
        @DisplayName("should clear context after successful filter chain execution")
        void doFilter_shouldClearContextAfterChain() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn("true");

            invokeFilter(request, response, filterChain);

            // After doFilter returns, context should be cleared
            assertFalse(PressureTestContext.isPressureTest(),
                    "PressureTestContext should be cleared after filter execution");
        }

        @Test
        @DisplayName("should clear context even when filter chain throws an exception")
        void doFilter_shouldClearContextAfterChainThrows() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn("true");
            doThrow(new RuntimeException("downstream failure"))
                    .when(filterChain).doFilter(any(), any());

            try {
                invokeFilter(request, response, filterChain);
            } catch (RuntimeException expected) {
                // expected — the filter propagates the exception
            }

            // Context must be cleared even on exception
            assertFalse(PressureTestContext.isPressureTest(),
                    "PressureTestContext should be cleared even when chain throws");
        }

        @Test
        @DisplayName("should clear context when chain throws a ServletException")
        void doFilter_shouldClearContextWhenChainThrowsServletException() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn("true");
            doThrow(new ServletException("servlet error"))
                    .when(filterChain).doFilter(any(), any());

            try {
                invokeFilter(request, response, filterChain);
            } catch (ServletException expected) {
                // expected
            }

            assertFalse(PressureTestContext.isPressureTest());
        }
    }

    @Nested
    @DisplayName("doFilterInternal — filter chain delegation")
    class FilterChainDelegation {

        @Test
        @DisplayName("should always call filterChain.doFilter regardless of header presence")
        void doFilter_shouldAlwaysCallFilterChain() throws Exception {
            when(request.getHeader(PressureTestContext.PRESSURE_TEST_HEADER)).thenReturn(null);

            invokeFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
