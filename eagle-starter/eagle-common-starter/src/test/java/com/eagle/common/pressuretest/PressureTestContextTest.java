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
        @DisplayName("标记：应设置压测测试Flag")
        void mark_shouldSetPressureTestFlag() {
            PressureTestContext.mark();

            assertTrue(PressureTestContext.isPressureTest());
        }

        @Test
        @DisplayName("默认State：应Befalse")
        void defaultState_shouldBeFalse() {
            assertFalse(PressureTestContext.isPressureTest());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("清理：应移除Flag")
        void clear_shouldRemoveFlag() {
            PressureTestContext.mark();
            assertTrue(PressureTestContext.isPressureTest());

            PressureTestContext.clear();

            assertFalse(PressureTestContext.isPressureTest());
        }

        @Test
        @DisplayName("清理：使用out标记时应Be安全")
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
        @DisplayName("应BeIsolatedBetweenThreads")
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
        @DisplayName("child线程标记应不Affect主线程线程")
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
