package com.eagle.system.message.infrastructure.messaging;

import com.eagle.rocketmq.events.SendUserMessageIntegrationEvent;
import com.eagle.rocketmq.listener.AbstractDlqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SendUserMessage DLQ 兜底——重试 16 次仍失败时进入此处。
 *
 * <p>本期记录 ERROR 日志（告警 stub），后续接入运维告警系统。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class SendUserMessageDlqListener extends AbstractDlqListener<SendUserMessageIntegrationEvent> {

    public SendUserMessageDlqListener(RocketMqProperties props) {
        super(props);
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
