package com.eagle.rocketmq.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RocketMqProperties")
class RocketMqPropertiesTest {

    @Test
    @DisplayName("兼容 eagle.rocketmq.name-server 绑定到 namesrvAddr")
    void shouldBindNameServerAliasToNamesrvAddr() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "eagle.rocketmq.name-server", "rocketmq-namesrv:9876"
        )));

        RocketMqProperties properties = binder.bind("eagle.rocketmq", RocketMqProperties.class).get();

        assertThat(properties.getNamesrvAddr()).isEqualTo("rocketmq-namesrv:9876");
    }
}
