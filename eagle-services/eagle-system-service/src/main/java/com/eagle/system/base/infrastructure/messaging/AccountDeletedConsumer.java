package com.eagle.system.base.infrastructure.messaging;

import com.eagle.rocketmq.listener.AbstractRocketMqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消费 auth-service 发布的"账号删除"集成事件,在 base 域级联删除对应 User。
 * <p>
 * topic {@code eagle_auth_events},tag {@code account.deleted}。
 * 幂等:{@code UserRepository.findByAccountId(...)} 找不到即跳过。
 * <p>
 * <strong>Topic 命名约定</strong>:与 auth-service 端 {@code AuthIntegrationEventPublisher.TOPIC}
 * 严格一致,故意<em>不</em>拼 {@code eagle.rocketmq.topic-env-prefix}。
 */
@Slf4j
@Component
public class AccountDeletedConsumer extends AbstractRocketMqListener<AccountDeletedMessage> {

    static final String TOPIC = "eagle_auth_events";
    static final String TAG = "account.deleted";
    static final String CONSUMER_GROUP = "system_account_deleted";

    private final AccountEventApplicationService accountEventService;

    public AccountDeletedConsumer(RocketMqProperties props,
                                  AccountEventApplicationService accountEventService) {
        super(props);
        this.accountEventService = accountEventService;
    }

    @Override
    protected String getTopic() {
        return TOPIC;
    }

    @Override
    protected Class<AccountDeletedMessage> getEventClass() {
        return AccountDeletedMessage.class;
    }

    @Override
    protected String getConsumerGroup() {
        return CONSUMER_GROUP;
    }

    @Override
    protected String getTagExpression() {
        return TAG;
    }

    @Override
    protected void handle(AccountDeletedMessage event) {
        accountEventService.onAccountDeleted(event);
    }
}
