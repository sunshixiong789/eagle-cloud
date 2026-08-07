package com.eagle.system.message.infrastructure.messaging;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.system.message.application.event.SendUserMessageMessage;
import com.eagle.system.message.application.service.SendMessageApplicationService;
import com.eagle.system.message.domain.model.MessageCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通用站内信发送事件消费者。
 *
 * <p>订阅 {@code user_message_send}，将
 * {@link SendUserMessageMessage} 落库为 {@code user_message} 表中一条记录。
 *
 * <p>幂等保障：以 {@code event.bizKey} 唯一去重——同 bizKey 的重复消息只会落库一条。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class SendUserMessageConsumer extends AbstractAmqpListener<SendUserMessageMessage> {

    /**
     * 通用站内信 topic。字面量而非引用共享常量 —— 跨服务契约就是这个字符串本身，
     * 生产方（trade / user 各模块）各自持有同一字面量，两侧不产生编译期依赖。
     */
    static final String USER_MESSAGE_SEND_TOPIC = "user_message_send";

    static final String CONSUMER_GROUP = "system_user_message_send";

    private final SendMessageApplicationService sendMessageApplicationService;

    public SendUserMessageConsumer(AmqpProperties props,
                                   SendMessageApplicationService sendMessageApplicationService) {
        super(props);
        this.sendMessageApplicationService = sendMessageApplicationService;
    }

    /**
     * 只返回<b>逻辑</b> topic 名，环境前缀由 {@code resolveExchangeName()} 统一拼。
     *
     * <p>此处曾手动拼 {@code props.getExchangePrefix()} —— 那是 RocketMQ 基类的遗留写法
     * （旧基类把 {@code getTopic()} 原样当最终 topic，不补前缀，故子类必须自己拼）。
     * 迁到 {@code AbstractAmqpListener} 后前缀由基类补，再手动拼一次会让本消费者
     * 绑到 {@code dev_dev_user_message_send}，而生产方发往 {@code dev_user_message_send} ——
     * 站内信集成事件会全部投进无队列绑定的 exchange 被静默丢弃。
     */
    @Override
    protected String getTopic() {
        return USER_MESSAGE_SEND_TOPIC;
    }

    @Override
    protected Class<SendUserMessageMessage> getEventClass() {
        return SendUserMessageMessage.class;
    }

    @Override
    protected String getConsumerGroup() {
        return CONSUMER_GROUP;
    }

    @Override
    protected void handle(SendUserMessageMessage event) {
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
