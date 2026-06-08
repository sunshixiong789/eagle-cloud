package com.eagle.system.base.infrastructure.messaging;

import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.application.service.SystemLogRecorder;
import com.eagle.system.base.infrastructure.messaging.event.AuthLoginMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthLoginConsumer")
class AuthLoginConsumerTest {

    @Mock
    private SystemLogRecorder recorder;

    private AuthLoginConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthLoginConsumer(new RocketMqProperties(), recorder);
    }

    @Test
    @DisplayName("topic/tag/consumerGroup 与登录日志事件契约对齐")
    void wiringMatchesConstants() {
        assertThat(consumer.getTopic()).isEqualTo("eagle_auth_events");
        assertThat(consumer.getTagExpression()).isEqualTo("auth.login");
        assertThat(consumer.getConsumerGroup()).isEqualTo("system_auth_login");
        assertThat(consumer.getEventClass()).isEqualTo(AuthLoginMessage.class);
    }

    @Test
    @DisplayName("handle 应转发到 SystemLogRecorder.recordLogin")
    void delegatesToRecorder() {
        AuthLoginMessage event = new AuthLoginMessage();
        event.setUsername("alice");

        consumer.handle(event);

        verify(recorder).recordLogin(event);
    }
}
