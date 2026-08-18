package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.system.base.domain.model.DeadLetterRecord;
import com.eagle.system.base.domain.repository.DeadLetterRecordRepository;
import com.eagle.system.base.application.event.AccountDeletedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * AccountDeleted DLQ 兜底 —— 重试耗尽（{@code spring.rabbitmq.listener.simple.retry.max-retries}）仍失败时进入此处。
 * <p>
 * 业务影响:auth-service 已删除 Account,但 base 域 User 未级联删除 — 残留孤儿数据,
 * 用户列表会显示已经销户的账号信息。
 * <p>
 * 双层兜底: {@link AlertService} 写结构化 ERROR + MDC 标签,运维 Logback WebhookAppender
 * 按 {@code alert.category=mq-dlq} 转钉钉/企微; 本类同时保留传统 log.error 留底文件。
 */
@Slf4j
@Component
public class AccountDeletedDlqListener extends AbstractDlqListener<AccountDeletedMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;
    private final DeadLetterRecordRepository deadLetterRepository;
    private final ObjectMapper objectMapper;

    public AccountDeletedDlqListener(AmqpProperties props,
                                     AlertService alertService,
                                     DeadLetterRecordRepository deadLetterRepository,
                                     ObjectMapper objectMapper) {
        super(props);
        this.alertService = alertService;
        this.deadLetterRepository = deadLetterRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected String getOriginalTopic() {
        return AccountDeletedConsumer.TOPIC;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return AccountDeletedConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<AccountDeletedMessage> getEventClass() {
        return AccountDeletedMessage.class;
    }

    @Override
    protected void handleDeadLetter(AccountDeletedMessage event, int totalAttempts) {
        log.error("[DLQ ALERT] account-deleted dead-letter: eventId={}, accountId={}, attempts={}",
                event.getEventId(), event.getAccountId(), totalAttempts);
        persistDeadLetter(event, totalAttempts);
        alertService.send(new AlertEvent(
                AlertSeverity.ERROR,
                ALERT_SOURCE,
                ALERT_CATEGORY,
                "AccountDeleted 死信投递",
                "base 域 User 未能级联删除,残留孤儿数据,需人工清理",
                Map.of(
                        "eventId", String.valueOf(event.getEventId()),
                        "accountId", String.valueOf(event.getAccountId()),
                        "totalAttempts", String.valueOf(totalAttempts)),
                null,
                null));
    }

    private void persistDeadLetter(AccountDeletedMessage event, int totalAttempts) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            deadLetterRepository.save(DeadLetterRecord.capture(
                    event.getEventId(),
                    AccountDeletedConsumer.TOPIC,
                    AccountDeletedConsumer.TAG,
                    AccountDeletedConsumer.CONSUMER_GROUP,
                    totalAttempts,
                    payload,
                    "base 域 User 级联删除失败 - 详见 MDC traceId 关联的业务异常"));
        } catch (RuntimeException ex) {
            log.error("persist dead letter failed, eventId={}", event.getEventId(), ex);
            throw ex;
        }
    }
}
