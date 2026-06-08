package com.eagle.datasource.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("DataSourceContextHolder")
class DataSourceContextHolderTest {

    @AfterEach
    void cleanup() {
        DataSourceContextHolder.clear();
    }

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("不设置时应返回主库")
        void shouldReturnMasterWhenNotSet() {
            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.get());
        }

        @Test
        @DisplayName("应返回设置值")
        void shouldReturnSetValue() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.get());
        }
    }

    @Nested
    @DisplayName("getRaw()")
    class GetRaw {

        @Test
        @DisplayName("不设置时应返回null")
        void shouldReturnNullWhenNotSet() {
            assertNull(DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("应返回主库后显式设置")
        void shouldReturnMasterAfterExplicitSet() {
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);
            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("应返回从库后设置")
        void shouldReturnSlaveAfterSet() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.getRaw());
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("应Make获取原始null后清理")
        void shouldMakeGetRawNullAfterClear() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            DataSourceContextHolder.clear();

            assertNull(DataSourceContextHolder.getRaw(),
                    "clear() must call remove() so thread pool threads don't carry stale context");
        }

        @Test
        @DisplayName("应恢复获取到主库后清理")
        void shouldRestoreGetToMasterAfterClear() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            DataSourceContextHolder.clear();

            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.get());
        }
    }
}
