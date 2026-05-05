package com.eagle.datasource.aspect;

import com.eagle.datasource.annotation.ReadOnly;
import com.eagle.datasource.routing.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadOnlyAspect")
class ReadOnlyAspectTest {

    private final ReadOnlyAspect aspect = new ReadOnlyAspect();

    @Mock
    ProceedingJoinPoint pjp;

    @AfterEach
    void cleanup() {
        DataSourceContextHolder.clear();
    }

    @Nested
    @DisplayName("@ReadOnly 切面")
    class AroundReadOnly {

        @Test
        @DisplayName("should switch context to SLAVE during proceed()")
        void shouldSwitchToSlaveDuringExecution() throws Throwable {
            AtomicReference<String> captured = new AtomicReference<>();
            when(pjp.proceed()).thenAnswer(inv -> {
                captured.set(DataSourceContextHolder.getRaw());
                return null;
            });

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals(DataSourceContextHolder.SLAVE, captured.get());
        }

        @Test
        @DisplayName("should call clear() (not set('master')) after execution — prevents thread pool leak")
        void shouldClearThreadLocalNotSetMasterWhenNoPriorValue() throws Throwable {
            // 关键回归：修复前 finally 会执行 set("master") 导致 ThreadLocal 永不清除
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertNull(DataSourceContextHolder.getRaw(),
                    "ThreadLocal must be removed via clear(), not set to 'master'");
        }

        @Test
        @DisplayName("should restore explicitly-set MASTER after execution")
        void shouldRestoreExplicitMasterAfterExecution() throws Throwable {
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("should restore SLAVE context when prior value was SLAVE")
        void shouldRestoreSlaveWhenPriorValueWasSlave() throws Throwable {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("should clear ThreadLocal even when proceed() throws")
        void shouldClearContextOnException() throws Throwable {
            when(pjp.proceed()).thenThrow(new RuntimeException("downstream failure"));

            assertThrows(RuntimeException.class,
                    () -> aspect.aroundReadOnly(pjp, readOnlyAnnotation()));
            assertNull(DataSourceContextHolder.getRaw(),
                    "ThreadLocal must be cleared even on exception");
        }

        @Test
        @DisplayName("should return value propagated from proceed()")
        void shouldReturnProceedResult() throws Throwable {
            when(pjp.proceed()).thenReturn("expected");

            Object result = aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals("expected", result);
        }
    }

    @Nested
    @DisplayName("@Transactional 切面")
    class AroundTransactional {

        @Test
        @DisplayName("should switch to SLAVE when readOnly = true")
        void shouldSwitchToSlaveWhenReadOnlyTrue() throws Throwable {
            AtomicReference<String> captured = new AtomicReference<>();
            when(pjp.proceed()).thenAnswer(inv -> {
                captured.set(DataSourceContextHolder.getRaw());
                return null;
            });

            aspect.aroundTransactional(pjp, readOnlyTxAnnotation());

            assertEquals(DataSourceContextHolder.SLAVE, captured.get());
        }

        @Test
        @DisplayName("should NOT touch context when readOnly = false")
        void shouldNotSwitchWhenReadOnlyFalse() throws Throwable {
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundTransactional(pjp, writeTxAnnotation());

            assertNull(DataSourceContextHolder.getRaw(),
                    "Write transaction must not alter DataSourceContextHolder");
        }

        @Test
        @DisplayName("should clear ThreadLocal after readOnly=true execution")
        void shouldClearAfterReadOnlyTxExecution() throws Throwable {
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundTransactional(pjp, readOnlyTxAnnotation());

            assertNull(DataSourceContextHolder.getRaw());
        }
    }

    // --- annotation helpers ---

    @ReadOnly
    static void annotatedWithReadOnly() {
    }

    @Transactional(readOnly = true)
    static void annotatedWithReadOnlyTx() {
    }

    @Transactional
    static void annotatedWithWriteTx() {
    }

    private static ReadOnly readOnlyAnnotation() throws NoSuchMethodException {
        return ReadOnlyAspectTest.class
                .getDeclaredMethod("annotatedWithReadOnly")
                .getAnnotation(ReadOnly.class);
    }

    private static Transactional readOnlyTxAnnotation() throws NoSuchMethodException {
        return ReadOnlyAspectTest.class
                .getDeclaredMethod("annotatedWithReadOnlyTx")
                .getAnnotation(Transactional.class);
    }

    private static Transactional writeTxAnnotation() throws NoSuchMethodException {
        return ReadOnlyAspectTest.class
                .getDeclaredMethod("annotatedWithWriteTx")
                .getAnnotation(Transactional.class);
    }
}
