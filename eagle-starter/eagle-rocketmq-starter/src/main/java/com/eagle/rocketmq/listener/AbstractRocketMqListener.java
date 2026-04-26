package com.eagle.rocketmq.listener;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 领域事件消费者抽象基类。
 *
 * <p>子类只需指定事件类型和 Topic，即可自动消费并反序列化。
 *
 * @param <T> 事件类型
 * @author 孙士雄
 */
@Slf4j
public abstract class AbstractRocketMqListener<T extends BaseEvent> implements InitializingBean, DisposableBean {

    private PushConsumer consumer;

    /**
     * 返回监听的 Topic。
     *
     * @return Topic 名称
     */
    protected abstract String getTopic();

    /**
     * 返回接入点地址。
     *
     * @return endpoints
     */
    protected abstract String getEndpoints();

    /**
     * 返回消费者组。
     *
     * @return consumer group
     */
    protected abstract String getConsumerGroup();

    /**
     * 返回事件类型 Class。
     *
     * @return 事件 Class
     */
    protected abstract Class<T> getEventClass();

    /**
     * 处理事件。
     *
     * @param event 反序列化后的事件对象
     */
    protected abstract void handle(T event);

    @Override
    public void afterPropertiesSet() throws Exception {
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
                .setEndpoints(getEndpoints())
                .build();
        FilterExpression filterExpression = new FilterExpression("*");
        consumer = provider.newPushConsumerBuilder()
                .setClientConfiguration(configuration)
                .setConsumerGroup(getConsumerGroup())
                .setSubscriptionExpressions(java.util.Collections.singletonMap(getTopic(), filterExpression))
                .setMessageListener(this::onMessage)
                .build();
        log.info("RocketMQ consumer started, topic: {}, group: {}", getTopic(), getConsumerGroup());
    }

    @Override
    public void destroy() throws Exception {
        if (consumer != null) {
            consumer.close();
            log.info("RocketMQ consumer closed, topic: {}", getTopic());
        }
    }

    private ConsumeResult onMessage(MessageView messageView) {
        String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        log.debug("RocketMQ message received, topic: {}, messageId: {}",
                messageView.getTopic(), messageView.getMessageId());
        try {
            T event = JSON.parseObject(body, getEventClass());
            handle(event);
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to handle RocketMQ message, topic: {}, body: {}", getTopic(), body, e);
            return ConsumeResult.FAILURE;
        }
    }
}
