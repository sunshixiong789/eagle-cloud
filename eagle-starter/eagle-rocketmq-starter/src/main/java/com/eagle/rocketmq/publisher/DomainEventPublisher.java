package com.eagle.rocketmq.publisher;

import com.eagle.common.event.BaseEvent;

/**
 * 领域事件发布器接口。
 *
 * <p>将领域事件序列化并发送到消息队列。
 *
 * @author 孙士雄
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件。
     *
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(T event);

    /**
     * 发布领域事件到指定 Topic。
     *
     * @param topic 目标 Topic
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(String topic, T event);
}
