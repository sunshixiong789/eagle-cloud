package com.eagle.amqp;

import com.eagle.amqp.config.AmqpListenerRegistrar;
import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.common.event.BaseEvent;
import com.eagle.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("主消费者启动校验")
class AmqpConsumerValidationTest {

    @Test
    @DisplayName("仍使用默认 consumerGroup 时启动失败")
    void shouldFailWhenDefaultConsumerGroupUsed() {
        var listener = new DefaultGroupConsumer(props());
        assertThatThrownBy(() -> AmqpListenerRegistrar.validateMainConsumers(List.of(listener)))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("两个主消费者绑同一 queue 时启动失败")
    void shouldFailWhenTwoListenersShareQueue() {
        AmqpProperties properties = props();
        assertThatThrownBy(() -> AmqpListenerRegistrar.validateMainConsumers(List.of(
                new NamedGroupConsumer(properties, "same-group"),
                new NamedGroupConsumer(properties, "same-group"))))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("各自覆盖 group 时通过")
    void shouldPassWhenGroupsAreUnique() {
        AmqpProperties properties = props();
        AmqpListenerRegistrar.validateMainConsumers(List.of(
                new NamedGroupConsumer(properties, "group-a"),
                new NamedGroupConsumer(properties, "group-b")));
    }

    private static AmqpProperties props() {
        AmqpProperties properties = new AmqpProperties();
        properties.setExchangePrefix("dev_");
        return properties;
    }

    static class SampleMessage extends BaseEvent {
    }

    static class DefaultGroupConsumer extends AbstractAmqpListener<SampleMessage> {
        DefaultGroupConsumer(AmqpProperties properties) {
            super(properties);
        }

        @Override
        protected String getTopic() {
            return "demo";
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handle(SampleMessage event) {
        }
    }

    static class NamedGroupConsumer extends AbstractAmqpListener<SampleMessage> {
        private final String group;

        NamedGroupConsumer(AmqpProperties properties, String group) {
            super(properties);
            this.group = group;
        }

        @Override
        protected String getTopic() {
            return "demo";
        }

        @Override
        protected String getConsumerGroup() {
            return group;
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handle(SampleMessage event) {
        }
    }
}
