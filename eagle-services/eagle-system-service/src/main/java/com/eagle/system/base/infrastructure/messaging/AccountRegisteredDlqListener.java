package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.rocketmq.listener.AbstractDlqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.domain.model.DeadLetterRecord;
import com.eagle.system.base.domain.repository.DeadLetterRecordRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * AccountRegistered DLQ 兜底 —— RocketMQ 重试 16 次仍失败时进入此处。
 * <p>
 * 业务影响:auth-service 已创建 Account,但 base 域 User 创建失败 — 数据不一致,
 * 后续登录时 {@code RemoteAuthorizationAdapter} 会查不到 user(返回 empty),
 * JWT claim 没有 name / roleCodes,权限失效。
 * <p>
 * 双层兜底: {@link AlertService} 写结构化 ERROR + MDC 标签,运维 Logback WebhookAppender
 * 按 {@code alert.category=mq-dlq} 转钉钉/企微; 本类同时保留传统 log.error 留底文件。
 */
@Slf4j
@Component
public class AccountRegisteredDlqListener extends AbstractDlqListener<AccountRegisteredMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;
    private final DeadLetterRecordRepository deadLetterRepository;
    private final ObjectMapper objectMapper;

    public AccountRegisteredDlqListener(RocketMqProperties props,
                                        AlertService alertService,
                                        DeadLetterRecordRepository deadLetterRepository,
                                        ObjectMapper objectMapper) {
        super(props);
        this.alertService = alertService;
        this.deadLetterRepository = deadLetterRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return AccountRegisteredConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<AccountRegisteredMessage> getEventClass() {
        return AccountRegisteredMessage.class;
    }

    @Override
    protected void handleDeadLetter(AccountRegisteredMessage event, int totalAttempts) {
        log.error("[DLQ ALERT] account-registered dead-letter: eventId={}, accountId={}, username={}, attempts={}",
                event.getEventId(), event.getAccountId(), event.getUsername(), totalAttempts);
        // 1) 落库 — 即使告警链路失败也留下原始证据
        persistDeadLetter(event, totalAttempts);
        // 2) 告警 — webhook 异常不影响落库
        alertService.send(new AlertEvent(
                AlertSeverity.ERROR,
                ALERT_SOURCE,
                ALERT_CATEGORY,
                "AccountRegistered 死信投递",
                "base 域 User 创建失败,可能导致登录鉴权降级,需人工补录",
                Map.of(
                        "eventId", String.valueOf(event.getEventId()),
                        "accountId", String.valueOf(event.getAccountId()),
                        "username", String.valueOf(event.getUsername()),
                        "totalAttempts", String.valueOf(totalAttempts)),
                null,
                null));
    }

    private void persistDeadLetter(AccountRegisteredMessage event, int totalAttempts) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            deadLetterRepository.save(DeadLetterRecord.capture(
                    event.getEventId(),
                    AccountRegisteredConsumer.TOPIC,
                    AccountRegisteredConsumer.TAG,
                    AccountRegisteredConsumer.CONSUMER_GROUP,
                    totalAttempts,
                    payload,
                    "base 域 User 创建失败 - 详见 MDC traceId 关联的业务异常"));
        } catch (RuntimeException ex) {
            // 落库失败不能阻塞 RocketMQ ack —— 至少 ERROR 日志 + 告警还在
            log.error("persist dead letter failed, eventId={}", event.getEventId(), ex);
        }
    }
}
