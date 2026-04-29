package com.eagle.common.pressuretest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PressureTestContext}.
 *
 * <p>Relies on ThreadLocal isolation — {@link PressureTestContext#clear()} is called
 * after each test to prevent thread pollution.
 */
class PressureTestContextTest {

    @AfterEach
    void tearDown() {
        // Always clear to prevent state leaking between tests in the same thread
        PressureTestContext.clear();
    }

    @Nested
    @DisplayName("mark and isPressureTest")
    class MarkAndCheck {

        @Test
        @DisplayName("should return true after mark() is called")
        void mark_shouldSetPressureTestFlag() {
            PressureTestContext.mark();

            assertTrue(PressureTestContext.isPressureTest());
        }

        @Test
        @DisplayName("should default to false when mark() has not been called")
        void defaultState_shouldBeFalse() {
            assertFalse(PressureTestContext.isPressureTest());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should return false after mark() followed by clear()")
        void clear_shouldRemoveFlag() {
            PressureTestContext.mark();
            assertTrue(PressureTestContext.isPressureTest());

            PressureTestContext.clear();

            assertFalse(PressureTestContext.isPressureTest());
        }

        @Test
        @DisplayName("should be safe to call clear() without a prior mark()")
        void clear_shouldBeSafeWithoutMark() {
            // Should not throw
            PressureTestContext.clear();

            assertFalse(PressureTestContext.isPressureTest());
        }
    }

    @Nested
    @DisplayName("thread isolation")
    class ThreadIsolation {

        @Test
        @DisplayName("marking in main thread should not affect child thread")
        void shouldBeIsolatedBetweenThreads() throws InterruptedException {
            PressureTestContext.mark();
            assertTrue(PressureTestContext.isPressureTest(), "Main thread should be marked");

            AtomicBoolean childThreadResult = new AtomicBoolean(true);
            CountDownLatch latch = new CountDownLatch(1);

            Thread child = new Thread(() -> {
                try {
                    // A fresh thread has its own ThreadLocal — should NOT inherit parent's value
                    childThreadResult.set(PressureTestContext.isPressureTest());
                } finally {
                    PressureTestContext.clear();
                    latch.countDown();
                }
            });
            child.start();
            latch.await();

            assertFalse(childThreadResult.get(),
                    "Child thread should NOT see the pressure test flag from the main thread");
        }

        @Test
        @DisplayName("marking in child thread should not affect main thread")
        void childThreadMarkShouldNotAffectMainThread() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);

            Thread child = new Thread(() -> {
                try {
                    PressureTestContext.mark();
                } finally {
                    // deliberately do NOT clear, to show main thread is not polluted
                    latch.countDown();
                }
            });
            child.start();
            latch.await();

            // Main thread should remain clean
            assertFalse(PressureTestContext.isPressureTest(),
                    "Main thread should not be affected by child thread mark");
        }
    }
}
