package com.eagle.system.base.infrastructure.messaging;

import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.common.util.LogMask;
import com.eagle.system.base.application.event.AuthLoginMessage;
import com.eagle.system.base.domain.model.DeadLetterRecord;
import com.eagle.system.base.domain.repository.DeadLetterRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * AuthLogin DLQ 兜底 —— {@link AuthLoginConsumer} 重试耗尽仍失败时进入此处。
 *
 * <p>业务影响：一条登录日志没能写入系统日志表，登录本身不受影响，
 * 但安全审计的登录记录会缺失。落库留证后可人工补录。
 *
 * <p>严重级取 {@link AlertSeverity#WARN} 而非 ERROR —— 审计缺一条记录不需要立刻叫醒人，
 * 与 {@link AccountRegisteredDlqListener}（数据不一致、影响鉴权）区分开，避免告警噪声。
 *
 * <p>{@code username} 在手机号账号下就是手机号，经 {@link LogMask} 脱敏后再落日志与告警。
 */
@Slf4j
@Component
public class AuthLoginDlqListener extends AbstractDlqListener<AuthLoginMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;
    private final DeadLetterRecordRepository deadLetterRepository;
    private final ObjectMapper objectMapper;

    public AuthLoginDlqListener(AmqpProperties props,
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
        return AuthLoginConsumer.TOPIC;
    }

    @Override
    protected String getOriginalConsumerGroup() {
        return AuthLoginConsumer.CONSUMER_GROUP;
    }

    @Override
    protected Class<AuthLoginMessage> getEventClass() {
        return AuthLoginMessage.class;
    }

    @Override
    protected void handleDeadLetter(AuthLoginMessage event, int totalAttempts) {
        log.error("[DLQ ALERT] auth-login dead-letter: eventId={}, accountId={}, username={}, attempts={}",
                event.getEventId(), event.getAccountId(),
                LogMask.phone(event.getUsername()), totalAttempts);
        persistDeadLetter(event, totalAttempts);
        alertService.send(new AlertEvent(
                AlertSeverity.WARN,
                ALERT_SOURCE,
                ALERT_CATEGORY,
                "AuthLogin 死信投递",
                "登录日志写入失败，安全审计记录缺失，需人工补录",
                Map.of(
                        "eventId", String.valueOf(event.getEventId()),
                        "accountId", String.valueOf(event.getAccountId()),
                        "username", LogMask.phone(event.getUsername()),
                        "totalAttempts", String.valueOf(totalAttempts)),
                null,
                null));
    }

    private void persistDeadLetter(AuthLoginMessage event, int totalAttempts) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            deadLetterRepository.save(DeadLetterRecord.capture(
                    event.getEventId(),
                    AuthLoginConsumer.TOPIC,
                    AuthLoginConsumer.TAG,
                    AuthLoginConsumer.CONSUMER_GROUP,
                    totalAttempts,
                    payload,
                    "登录日志写入失败 - 详见 MDC traceId 关联的业务异常"));
        } catch (RuntimeException ex) {
            log.error("persist dead letter failed, eventId={}", event.getEventId(), ex);
            throw ex;
        }
    }
}
