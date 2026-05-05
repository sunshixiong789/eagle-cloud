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
        @DisplayName("should return MASTER when not explicitly set")
        void shouldReturnMasterWhenNotSet() {
            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.get());
        }

        @Test
        @DisplayName("should return SLAVE after set to SLAVE")
        void shouldReturnSetValue() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.get());
        }
    }

    @Nested
    @DisplayName("getRaw()")
    class GetRaw {

        @Test
        @DisplayName("should return null when not set — distinguishes from explicit MASTER")
        void shouldReturnNullWhenNotSet() {
            assertNull(DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("should return raw value after explicit set to MASTER")
        void shouldReturnMasterAfterExplicitSet() {
            DataSourceContextHolder.set(DataSourceContextHolder.MASTER);
            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getRaw());
        }

        @Test
        @DisplayName("should return SLAVE after set to SLAVE")
        void shouldReturnSlaveAfterSet() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
            assertEquals(DataSourceContextHolder.SLAVE, DataSourceContextHolder.getRaw());
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("should make getRaw() return null — ThreadLocal properly removed")
        void shouldMakeGetRawNullAfterClear() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            DataSourceContextHolder.clear();

            assertNull(DataSourceContextHolder.getRaw(),
                    "clear() must call remove() so thread pool threads don't carry stale context");
        }

        @Test
        @DisplayName("should restore get() to MASTER after clear")
        void shouldRestoreGetToMasterAfterClear() {
            DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);

            DataSourceContextHolder.clear();

            assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.get());
        }
    }
}
