package com.eagle.system.message.infrastructure.messaging;

import com.eagle.rocketmq.events.CommonMessageTopics;
import com.eagle.rocketmq.events.SendUserMessageIntegrationEvent;
import com.eagle.rocketmq.listener.AbstractRocketMqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import com.eagle.system.message.application.service.SendMessageApplicationService;
import com.eagle.system.message.domain.model.MessageCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 通用站内信发送事件消费者。
 *
 * <p>订阅 {@link CommonMessageTopics#USER_MESSAGE_SEND}，将
 * {@link SendUserMessageIntegrationEvent} 落库为 {@code user_message} 表中一条记录。
 *
 * <p>幂等保障：以 {@code event.bizKey} 唯一去重——同 bizKey 的重复消息只会落库一条。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eagle.rocketmq.enabled", havingValue = "true")
public class SendUserMessageConsumer extends AbstractRocketMqListener<SendUserMessageIntegrationEvent> {

    static final String CONSUMER_GROUP = "system_user_message_send";

    private final SendMessageApplicationService sendMessageApplicationService;
    private final String topicPrefix;

    public SendUserMessageConsumer(RocketMqProperties props,
                                   SendMessageApplicationService sendMessageApplicationService,
                                   Environment env) {
        super(props);
        this.sendMessageApplicationService = sendMessageApplicationService;
        this.topicPrefix = env.getProperty("eagle.rocketmq.topic-env-prefix", "dev_");
    }

    @Override
    protected String getTopic() {
        return topicPrefix + CommonMessageTopics.USER_MESSAGE_SEND;
    }

    @Override
    protected Class<SendUserMessageIntegrationEvent> getEventClass() {
        return SendUserMessageIntegrationEvent.class;
    }

    @Override
    protected String getConsumerGroup() {
        return CONSUMER_GROUP;
    }

    @Override
    protected void handle(SendUserMessageIntegrationEvent event) {
        if (event.getUserId() == null
                || event.getTitle() == null || event.getTitle().isBlank()
                || event.getContent() == null || event.getContent().isBlank()) {
            log.warn("send-user-message event invalid, skipped: eventId={}, userId={}, title={}",
                    event.getEventId(), event.getUserId(), event.getTitle());
            return;
        }
        MessageCategory category = MessageCategory.parse(event.getCategory());
        sendMessageApplicationService.send(
                event.getUserId(), category, event.getTitle(), event.getContent(), event.getBizKey());
    }
}
