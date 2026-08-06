package com.eagle.amqp.publisher;

import com.eagle.common.event.BaseEvent;

/**
 * 领域事件发布器接口。
 *
 * <p>把领域事件序列化后投递到 AMQP topic exchange。
 *
 * <p><b>相比原 RocketMQ 版本，刻意收窄为两个方法</b>。原接口有 9 个方法
 * （含 {@code publishAsync} / {@code publishDelayed} / {@code publishOrdered}），
 * 但两个仓库的业务代码<b>一处都没调用过</b>，而这三者恰是 RabbitMQ 的短板
 * （分别需要自封装 confirm、社区插件、单队列单消费者）。
 * 直接删除而非留空实现 —— 将来若误用会是编译错误，比运行期抛
 * {@code UnsupportedOperationException} 更早暴露。
 *
 * @author eagle
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件到指定 exchange，不指定 routing key（消费方按 {@code #} 全量订阅）。
     *
     * @param topic 目标 exchange 名（不含环境前缀，由实现补齐）
     * @param event 领域事件
     * @param <T>   事件类型
     */
    <T extends BaseEvent> void publish(String topic, T event);

    /**
     * 发布领域事件到指定 exchange，并附加 routing key 供消费侧精细过滤。
     *
     * <p>对应原 RocketMQ 的 tag —— 现网 tag 值形如 {@code "paid"} /
     * {@code "account.registered"}，本就是合法的 AMQP routing key，语义直接平移。
     *
     * @param topic      目标 exchange 名（不含环境前缀）
     * @param routingKey routing key（原 tag）
     * @param event      领域事件
     * @param <T>        事件类型
     */
    <T extends BaseEvent> void publish(String topic, String routingKey, T event);
}
