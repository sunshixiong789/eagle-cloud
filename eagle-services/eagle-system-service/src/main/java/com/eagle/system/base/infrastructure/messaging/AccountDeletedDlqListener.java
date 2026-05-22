package com.eagle.system.base.infrastructure.messaging;

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
 * 本期记录 ERROR 日志(告警 stub),后续接入告警系统 + 死信表持久化。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class AccountDeletedDlqListener extends AbstractDlqListener<AccountDeletedMessage> {

    public AccountDeletedDlqListener(RocketMqProperties props) {
        super(props);
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
        // TODO 接入告警系统 + 持久化到 t_dead_letter 表供人工清理孤儿 User
    }
}
