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

    @ReadOnly
    static void annotatedWithReadOnly() {
    }

    @Transactional(readOnly = true)
    static void annotatedWithReadOnlyTx() {
    }

    @Transactional
    static void annotatedWithWriteTx() {
    }

    // --- annotation helpers ---

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

    @AfterEach
    void cleanup() {
        DataSourceContextHolder.clear();
    }

    @Nested
    @DisplayName("@ReadOnly 切面")
    class AroundReadOnly {

        @Test
        @DisplayName("应切换到从库During执行")
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
        @DisplayName("无Prior值时应清理线程本地不设置主库")
        void shouldClearThreadLocalNotSetMasterWhenNoPriorValue() throws Throwable {
            // 关键回归：修复前 finally 会执行 set("master") 导致 ThreadLocal 永不清除
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertNull(DataSourceContextHolder.getRaw(),
                    "ThreadLocal must be removed via clear(), not set to 'master'");
        }

        @Test
        @DisplayName("应恢复显式主库后执行")
        void shouldRestoreExplicitMasterAfterExecution() throws Throwable {
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("Prior值Was从库时应恢复从库")
        void shouldRestoreSlaveWhenPriorValueWasSlave() throws Throwable {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundReadOnly(pjp, readOnlyAnnotation());

            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("异常时应清理上下文")
        void shouldClearContextOnException() throws Throwable {
            when(pjp.proceed()).thenThrow(new RuntimeException("downstream failure"));

            assertThrows(RuntimeException.class,
                    () -> aspect.aroundReadOnly(pjp, readOnlyAnnotation()));
            assertNull(DataSourceContextHolder.getRaw(),
                    "ThreadLocal must be cleared even on exception");
        }

        @Test
        @DisplayName("应返回执行结果")
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
        @DisplayName("读取仅true时应切换到从库")
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
        @DisplayName("读取仅false时不应切换")
        void shouldNotSwitchWhenReadOnlyFalse() throws Throwable {
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundTransactional(pjp, writeTxAnnotation());

            assertNull(DataSourceContextHolder.getRaw(),
                    "Write transaction must not alter DataSourceContextHolder");
        }

        @Test
        @DisplayName("lyTx执行时应清理后读取")
        void shouldClearAfterReadOnlyTxExecution() throws Throwable {
            when(pjp.proceed()).thenReturn(null);

            aspect.aroundTransactional(pjp, readOnlyTxAnnotation());

            assertNull(DataSourceContextHolder.getRaw());
        }
    }
}
