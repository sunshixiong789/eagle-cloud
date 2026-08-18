package com.eagle.amqp;

import com.eagle.amqp.support.EagleAmqpRetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("消费侧重试判定")
class EagleAmqpRetryTest {

    @Test
    @DisplayName("业务异常应重试")
    void shouldRetryBusinessFailure() {
        assertThat(EagleAmqpRetry.shouldRetry(new IllegalStateException("db down"))).isTrue();
    }

    @Test
    @DisplayName("反序列化失败即使被容器包一层也不重试")
    void shouldSkipRetryWhenDeserializationWrapped() {
        Message message = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8)).build();
        var wrapped = new ListenerExecutionFailedException("listener failed",
                new AmqpRejectAndDontRequeueException("bad json"), message);
        assertThat(EagleAmqpRetry.shouldRetry(wrapped)).isFalse();
    }

    @Test
    @DisplayName("强制回队不走进退避")
    void shouldSkipRetryWhenImmediateRequeue() {
        assertThat(EagleAmqpRetry.shouldRetry(new ImmediateRequeueAmqpException("stop")))
                .isFalse();
    }
}
