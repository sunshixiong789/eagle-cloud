package com.eagle.system.base.infrastructure.messaging;

import com.eagle.common.alert.AlertEvent;
import com.eagle.common.alert.AlertService;
import com.eagle.common.alert.AlertSeverity;
import com.eagle.rocketmq.listener.AbstractDlqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AccountDeleted DLQ 兜底 —— RocketMQ 重试 16 次仍失败时进入此处。
 * <p>
 * 业务影响:auth-service 已删除 Account,但 base 域 User 未级联删除 — 残留孤儿数据,
 * 用户列表会显示已经销户的账号信息。
 * <p>
 * 双层兜底: {@link AlertService} 写结构化 ERROR + MDC 标签,运维 Logback WebhookAppender
 * 按 {@code alert.category=mq-dlq} 转钉钉/企微; 本类同时保留传统 log.error 留底文件。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class AccountDeletedDlqListener extends AbstractDlqListener<AccountDeletedMessage> {

    private static final String ALERT_SOURCE = "eagle-system-service";
    private static final String ALERT_CATEGORY = "mq-dlq";

    private final AlertService alertService;

    public AccountDeletedDlqListener(RocketMqProperties props, AlertService alertService) {
        super(props);
        this.alertService = alertService;
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
        alertService.send(AlertEvent.builder()
                .severity(AlertSeverity.ERROR)
                .source(ALERT_SOURCE)
                .category(ALERT_CATEGORY)
                .title("AccountDeleted 死信投递")
                .message("base 域 User 未能级联删除,残留孤儿数据,需人工清理")
                .context("eventId", String.valueOf(event.getEventId()))
                .context("accountId", String.valueOf(event.getAccountId()))
                .context("totalAttempts", String.valueOf(totalAttempts))
                .build());
        // TODO 持久化到 t_dead_letter 表供人工清理孤儿 User(独立 PR)
    }
}
