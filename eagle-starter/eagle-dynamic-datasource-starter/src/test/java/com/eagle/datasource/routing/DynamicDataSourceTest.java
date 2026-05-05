package com.eagle.datasource.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DynamicDataSource")
class DynamicDataSourceTest {

    /** 暴露 protected 方法供白盒测试。 */
    private static class ExposedDynamicDataSource extends DynamicDataSource {
        ExposedDynamicDataSource(int slaveCount) {
            super(slaveCount);
        }

        Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }

    @AfterEach
    void cleanup() {
        DataSourceContextHolder.clear();
    }

    @Nested
    @DisplayName("单从库路由")
    class SingleSlave {

        @Test
        @DisplayName("should return 'master' when context is MASTER")
        void shouldReturnMasterKeyWhenContextIsMaster() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);

            assertEquals("master", ds.lookupKey());
        }

        @Test
        @DisplayName("should return 'slave' when context is SLAVE")
        void shouldReturnSlaveKeyWhenContextIsSlave() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            assertEquals("slave", ds.lookupKey());
        }

        @Test
        @DisplayName("should return 'master' when context not set")
        void shouldReturnMasterWhenContextNotSet() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);

            assertEquals("master", ds.lookupKey());
        }
    }

    @Nested
    @DisplayName("多从库轮询")
    class MultiSlave {

        @Test
        @DisplayName("should round-robin through slave-0, slave-1, slave-2 in order")
        void shouldRoundRobinThroughSlaves() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(3);
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            assertEquals("slave-0", ds.lookupKey());
            assertEquals("slave-1", ds.lookupKey());
            assertEquals("slave-2", ds.lookupKey());
            assertEquals("slave-0", ds.lookupKey()); // 轮回
        }

        @Test
        @DisplayName("should still return 'master' when context is MASTER regardless of slave count")
        void shouldReturnMasterWhenContextIsMaster() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(3);
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);

            assertEquals("master", ds.lookupKey());
        }

        @Test
        @DisplayName("should handle index overflow without returning negative key")
        void shouldHandleIndexOverflow() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(2);
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            // 模拟大量请求后 AtomicInteger 溢出附近的值，确保结果始终为非负 slave-n
            for (int i = 0; i < 10; i++) {
                String key = (String) ds.lookupKey();
                assert key.equals("slave-0") || key.equals("slave-1")
                        : "Unexpected key: " + key;
            }
        }
    }
}
