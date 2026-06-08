package com.eagle.datasource.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DynamicDataSource")
class DynamicDataSourceTest {

    @AfterEach
    void cleanup() {
        DataSourceContextHolder.clear();
    }

    /**
     * 暴露 protected 方法供白盒测试。
     */
    private static class ExposedDynamicDataSource extends DynamicDataSource {
        ExposedDynamicDataSource(int slaveCount) {
            super(slaveCount);
        }

        Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }

    @Nested
    @DisplayName("单从库路由")
    class SingleSlave {

        @Test
        @DisplayName("上下文Is主库时应返回主库key")
        void shouldReturnMasterKeyWhenContextIsMaster() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);

            assertEquals("master", ds.lookupKey());
        }

        @Test
        @DisplayName("上下文Is从库时应返回从库key")
        void shouldReturnSlaveKeyWhenContextIsSlave() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            assertEquals("slave", ds.lookupKey());
        }

        @Test
        @DisplayName("上下文不设置时应返回主库")
        void shouldReturnMasterWhenContextNotSet() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(1);

            assertEquals("master", ds.lookupKey());
        }
    }

    @Nested
    @DisplayName("多从库轮询")
    class MultiSlave {

        @Test
        @DisplayName("应轮询Robin穿过从库")
        void shouldRoundRobinThroughSlaves() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(3);
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            assertEquals("slave-0", ds.lookupKey());
            assertEquals("slave-1", ds.lookupKey());
            assertEquals("slave-2", ds.lookupKey());
            assertEquals("slave-0", ds.lookupKey()); // 轮回
        }

        @Test
        @DisplayName("上下文Is主库时应返回主库")
        void shouldReturnMasterWhenContextIsMaster() {
            ExposedDynamicDataSource ds = new ExposedDynamicDataSource(3);
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);

            assertEquals("master", ds.lookupKey());
        }

        @Test
        @DisplayName("应HandleIndexOverflow")
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
