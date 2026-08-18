package com.eagle.system.message.infrastructure.messaging;

import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.system.message.application.event.SendUserMessageMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SendUserMessage DLQ 兜底——重试耗尽（{@code spring.rabbitmq.listener.simple.retry.max-retries}）仍失败时进入此处。
 *
 * <p>message 模块与 base 隔离，不能写 {@code sys_dead_letter}；这里走 {@link AlertService}
 * 结构化告警，落库由运维侧 webhook / 日志采集承接。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class SendUserMessageDlqListener extends AbstractDlqListener<SendUserMessageMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;

    public SendUserMessageDlqListener(AmqpProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
    }

    /** 逻辑 topic 名，环境前缀由基类拼 —— 与 {@link SendUserMessageConsumer#getTopic()} 保持一致。 */
    @Override
    protected String getOriginalTopic() {
        return SendUserMessageConsumer.USER_MESSAGE_SEND_TOPIC;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return SendUserMessageConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<SendUserMessageMessage> getEventClass() {
        return SendUserMessageMessage.class;
    }

    @Override
    protected void handleDeadLetter(SendUserMessageMessage event, int totalAttempts) {
        log.error("[DLQ ALERT] send-user-message dead-letter: eventId={}, userId={}, bizKey={}, attempts={}",
                event.getEventId(), event.getUserId(), event.getBizKey(), totalAttempts);
        alertService.send(new AlertEvent(
                AlertSeverity.ERROR,
                ALERT_SOURCE,
                ALERT_CATEGORY,
                "SendUserMessage 死信投递",
                "站内信落库失败，用户可能收不到消息，需人工补发",
                Map.of(
                        "eventId", String.valueOf(event.getEventId()),
                        "userId", String.valueOf(event.getUserId()),
                        "bizKey", String.valueOf(event.getBizKey()),
                        "totalAttempts", String.valueOf(totalAttempts)),
                null,
                null));
    }
}
