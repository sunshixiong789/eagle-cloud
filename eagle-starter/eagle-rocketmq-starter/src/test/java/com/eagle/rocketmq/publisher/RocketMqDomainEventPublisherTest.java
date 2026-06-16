package com.eagle.rocketmq.publisher;

import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.admin.RocketMqTopicAdmin;
import com.eagle.rocketmq.properties.RocketMqProperties;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RocketMqDomainEventPublisher")
class RocketMqDomainEventPublisherTest {

    @Mock
    private RocketMqTopicAdmin topicAdmin;
    @Mock
    private ClientServiceProvider provider;
    @Mock
    private Producer producer;
    @Mock
    private MessageBuilder messageBuilder;
    @Mock
    private Message message;
    @Mock
    private SendReceipt receipt;
    @Mock
    private MessageId messageId;

    private RocketMqDomainEventPublisher publisher;

    @BeforeEach
    void setUp() throws Exception {
        publisher = new RocketMqDomainEventPublisher(new RocketMqProperties());
        ReflectionTestUtils.setField(publisher, "provider", provider);
        ReflectionTestUtils.setField(publisher, "producer", producer);
        ReflectionTestUtils.setField(publisher, "topicAdmin", topicAdmin);

        when(provider.newMessageBuilder()).thenReturn(messageBuilder);
        when(messageBuilder.setTopic("payment_transfer_events")).thenReturn(messageBuilder);
        when(messageBuilder.setBody(any(byte[].class))).thenReturn(messageBuilder);
        when(messageBuilder.setKeys("evt-1")).thenReturn(messageBuilder);
        when(messageBuilder.setTag("failed")).thenReturn(messageBuilder);
        when(messageBuilder.build()).thenReturn(message);
        when(producer.send(message)).thenReturn(receipt);
        when(receipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("msg-1");
    }

    @Test
    @DisplayName("同步发布显式 topic 前应先幂等建 topic")
    void shouldEnsureExplicitTopicBeforeSyncPublish() throws Exception {
        TestEvent event = new TestEvent();
        event.setEventId("evt-1");

        publisher.publish("payment_transfer_events", "failed", event);

        InOrder inOrder = inOrder(topicAdmin, producer);
        inOrder.verify(topicAdmin).ensureTopic("payment_transfer_events");
        inOrder.verify(producer).send(message);
    }

    private static class TestEvent extends BaseEvent {
    }
}
