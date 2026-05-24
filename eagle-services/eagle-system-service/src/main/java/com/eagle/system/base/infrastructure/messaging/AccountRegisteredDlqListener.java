package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.rocketmq.listener.AbstractDlqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class AccountRegisteredDlqListener extends AbstractDlqListener<AccountRegisteredMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;

    public AccountRegisteredDlqListener(RocketMqProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
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
        alertService.send(AlertEvent.builder()
                .severity(AlertSeverity.ERROR)
                .source(ALERT_SOURCE)
                .category(ALERT_CATEGORY)
                .title("AccountRegistered 死信投递")
                .message("base 域 User 创建失败,可能导致登录鉴权降级,需人工补录")
                .context("eventId", String.valueOf(event.getEventId()))
                .context("accountId", String.valueOf(event.getAccountId()))
                .context("username", String.valueOf(event.getUsername()))
                .context("totalAttempts", String.valueOf(totalAttempts))
                .build());
        // TODO 持久化到 t_dead_letter 表供人工补录(独立 PR)
    }
}
