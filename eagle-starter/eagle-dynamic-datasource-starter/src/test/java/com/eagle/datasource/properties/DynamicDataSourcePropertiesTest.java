package com.eagle.datasource.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DynamicDataSourceProperties.resolveSlaves()")
class DynamicDataSourcePropertiesTest {

    @Nested
    @DisplayName("单从库（slave 字段）")
    class SingleSlave {

        @Test
        @DisplayName("应返回单个从库从字段")
        void shouldReturnSingleSlaveFromField() {
            DynamicDataSourceProperties props = new DynamicDataSourceProperties();
            props.getSlave().setUrl("jdbc:mysql://slave:3306/db");

            List<DynamicDataSourceProperties.SingleDataSource> result = props.resolveSlaves();

            assertEquals(1, result.size());
            assertEquals("jdbc:mysql://slave:3306/db", result.get(0).getUrl());
        }

        @Test
        @DisplayName("从库URLnull时应返回空")
        void shouldReturnEmptyWhenSlaveUrlNull() {
            DynamicDataSourceProperties props = new DynamicDataSourceProperties();

            assertTrue(props.resolveSlaves().isEmpty());
        }

        @Test
        @DisplayName("从库URL空白时应返回空")
        void shouldReturnEmptyWhenSlaveUrlBlank() {
            DynamicDataSourceProperties props = new DynamicDataSourceProperties();
            props.getSlave().setUrl("   ");

            assertTrue(props.resolveSlaves().isEmpty());
        }
    }

    @Nested
    @DisplayName("多从库（slaves 列表）")
    class MultiSlaves {

        @Test
        @DisplayName("应返回从库列表")
        void shouldReturnSlavesList() {
            DynamicDataSourceProperties props = new DynamicDataSourceProperties();
            DynamicDataSourceProperties.SingleDataSource s0 = new DynamicDataSourceProperties.SingleDataSource();
            s0.setUrl("jdbc:mysql://slave0:3306/db");
            DynamicDataSourceProperties.SingleDataSource s1 = new DynamicDataSourceProperties.SingleDataSource();
            s1.setUrl("jdbc:mysql://slave1:3306/db");
            props.setSlaves(List.of(s0, s1));

            List<DynamicDataSourceProperties.SingleDataSource> result = props.resolveSlaves();

            assertEquals(2, result.size());
            assertEquals("jdbc:mysql://slave0:3306/db", result.get(0).getUrl());
            assertEquals("jdbc:mysql://slave1:3306/db", result.get(1).getUrl());
        }

        @Test
        @DisplayName("应优先使用从库列表超过单个从库字段")
        void shouldPreferSlavesListOverSingleSlaveField() {
            DynamicDataSourceProperties props = new DynamicDataSourceProperties();
            props.getSlave().setUrl("jdbc:mysql://slave-single:3306/db");

            DynamicDataSourceProperties.SingleDataSource s0 = new DynamicDataSourceProperties.SingleDataSource();
            s0.setUrl("jdbc:mysql://slave-multi:3306/db");
            props.setSlaves(List.of(s0));

            List<DynamicDataSourceProperties.SingleDataSource> result = props.resolveSlaves();

            assertEquals(1, result.size());
            assertEquals("jdbc:mysql://slave-multi:3306/db", result.get(0).getUrl(),
                    "slaves list must take precedence over single slave field");
        }
    }
}
