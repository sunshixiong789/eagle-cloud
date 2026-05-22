package com.eagle.system.base.infrastructure.messaging;

import com.eagle.rocketmq.listener.AbstractRocketMqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 消费 auth-service 发布的"账号注册"集成事件,在 base 域创建对应 User。
 * <p>
 * topic {@code eagle.auth.events},tag {@code account.registered}。
 * 幂等:依赖 {@code UserRepository.existsByAccountId(...)} 双重保护。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class AccountRegisteredConsumer extends AbstractRocketMqListener<AccountRegisteredMessage> {

    static final String TOPIC = "eagle.auth.events";
    static final String TAG = "account.registered";
    static final String CONSUMER_GROUP = "system_account_registered";

    private final AccountEventApplicationService accountEventService;

    public AccountRegisteredConsumer(RocketMqProperties props,
                                     AccountEventApplicationService accountEventService) {
        super(props);
        this.accountEventService = accountEventService;
    }

    @Override
    protected String getTopic() {
        return TOPIC;
    }

    @Override
    protected Class<AccountRegisteredMessage> getEventClass() {
        return AccountRegisteredMessage.class;
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
    protected void handle(AccountRegisteredMessage event) {
        accountEventService.onAccountRegistered(event);
    }
}
