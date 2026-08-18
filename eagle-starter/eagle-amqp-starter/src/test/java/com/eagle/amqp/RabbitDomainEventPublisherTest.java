package com.eagle.amqp;

import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.publisher.RabbitDomainEventPublisher;
import com.eagle.common.event.BaseEvent;
import com.eagle.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("发布等待 broker confirm")
class RabbitDomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RabbitAdmin rabbitAdmin;
    @Mock
    private ConnectionFactory connectionFactory;

    private RabbitDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitDomainEventPublisher(
                rabbitTemplate, rabbitAdmin, connectionFactory, new AmqpProperties(), new ObjectMapper());
    }

    @Test
    @DisplayName("broker ack 且无 return 时发布成功")
    void shouldSucceedWhenBrokerAcks() {
        completeConfirm(true, false);
        publisher.publish("demo", new SampleEvent());
    }

    @Test
    @DisplayName("broker nack 时抛 PUBLISH_FAILED")
    void shouldFailWhenBrokerNacks() {
        completeConfirm(false, false);
        assertThatThrownBy(() -> publisher.publish("demo", new SampleEvent()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("不可路由时抛 PUBLISH_FAILED")
    void shouldFailWhenUnroutable() {
        completeConfirm(true, true);
        assertThatThrownBy(() -> publisher.publish("demo", new SampleEvent()))
                .isInstanceOf(ServiceException.class);
    }

    private void completeConfirm(boolean ack, boolean returned) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            if (returned) {
                Message message = invocation.getArgument(2);
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "ex", "rk"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "disk full"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    static class SampleEvent extends BaseEvent {
    }
}
