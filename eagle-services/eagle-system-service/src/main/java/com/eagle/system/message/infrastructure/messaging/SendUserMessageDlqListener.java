package com.eagle.system.message.infrastructure.messaging;

import com.eagle.amqp.events.CommonMessageTopics;
import com.eagle.amqp.events.SendUserMessageIntegrationEvent;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SendUserMessage DLQ 兜底——重试耗尽（{@code eagle.amqp.consumer.max-attempts}）仍失败时进入此处。
 *
 * <p>本期记录 ERROR 日志（告警 stub），后续接入运维告警系统。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class SendUserMessageDlqListener extends AbstractDlqListener<SendUserMessageIntegrationEvent> {

    public SendUserMessageDlqListener(AmqpProperties props) {
        super(props);
    }

    /** 逻辑 topic 名，环境前缀由基类拼 —— 与 {@link SendUserMessageConsumer#getTopic()} 保持一致。 */
    @Override
    protected String getOriginalTopic() {
        return CommonMessageTopics.USER_MESSAGE_SEND;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return SendUserMessageConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<SendUserMessageIntegrationEvent> getEventClass() {
        return SendUserMessageIntegrationEvent.class;
    }

    @Override
    protected void handleDeadLetter(SendUserMessageIntegrationEvent event, int totalAttempts) {
        log.error("[DLQ ALERT] send-user-message dead-letter: eventId={}, userId={}, bizKey={}, attempts={}",
                event.getEventId(), event.getUserId(), event.getBizKey(), totalAttempts);
        // TODO 接入告警系统（钉钉/企微/邮件）
    }
}
