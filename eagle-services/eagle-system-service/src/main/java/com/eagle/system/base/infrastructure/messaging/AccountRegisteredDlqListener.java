package com.eagle.system.base.infrastructure.messaging;

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
 * 本期记录 ERROR 日志(告警 stub),后续接入告警系统 + 死信表持久化。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class AccountRegisteredDlqListener extends AbstractDlqListener<AccountRegisteredMessage> {

    public AccountRegisteredDlqListener(RocketMqProperties props) {
        super(props);
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
        // TODO 接入告警系统(钉钉/企微/邮件) + 持久化到 t_dead_letter 表供人工补录
    }
}
