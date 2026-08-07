package com.eagle.system.base.infrastructure.messaging;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.application.event.AccountRegisteredMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消费 auth-service 发布的"账号注册"集成事件,在 base 域创建对应 User。
 * <p>
 * exchange {@code eagle_auth_events},routing key {@code account.registered}。
 * 幂等:依赖 {@code UserRepository.existsByAccountId(...)} 双重保护。
 * <p>
 * <strong>Topic 命名约定</strong>:与 auth-service 端 {@code AuthIntegrationEventPublisher.TOPIC}
 * 严格一致,故意<em>不</em>拼 {@code eagle.amqp.exchange-prefix}(同进程的
 * {@code SendUserMessageConsumer} 拼 prefix 是另一条独立约定,不混用)。
 */
@Slf4j
@Component
public class AccountRegisteredConsumer extends AbstractAmqpListener<AccountRegisteredMessage> {

    static final String TOPIC = "eagle_auth_events";
    static final String TAG = "account.registered";
    static final String CONSUMER_GROUP = "system_account_registered";

    private final AccountEventApplicationService accountEventService;

    public AccountRegisteredConsumer(AmqpProperties props,
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
    protected String getRoutingKey() {
        return TAG;
    }

    @Override
    protected void handle(AccountRegisteredMessage event) {
        accountEventService.onAccountRegistered(event);
    }
}
