package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Redisson 发布/订阅工具类。
 *
 * <p>基于 {@link RTopic} 实现轻量级分布式事件广播，适用于：
 * <ul>
 *   <li>缓存失效广播（多节点同步清除本地缓存）</li>
 *   <li>配置变更通知（实时推送配置更新到所有节点）</li>
 *   <li>简单的节点间消息通知（不需要 MQ 的轻量场景）</li>
 * </ul>
 *
 * <p><b>与 RocketMQ 的选择建议：</b>
 * <ul>
 *   <li>需要消息持久化、消费确认、死信队列 → 使用 RocketMQ</li>
 *   <li>仅需实时广播、允许消息丢失（如缓存刷新）→ 使用本工具</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonTopicUtil {

    private final RedissonClient redissonClient;

    /**
     * 维护订阅 ID，用于精确取消订阅
     */
    private final ConcurrentMap<String, Integer> listenerIds = new ConcurrentHashMap<>();

    /**
     * 发布消息到指定 topic，所有订阅该 topic 的节点均会收到。
     *
     * <pre>
     * // 广播缓存失效通知
     * topicUtil.publish("cache:evict:user", userId);
     * </pre>
     *
     * @param topic   topic 名称
     * @param message 消息内容（需可序列化）
     * @param <T>     消息类型
     * @return 接收到消息的订阅者数量
     */
    public <T> long publish(String topic, T message) {
        long subscribers = redissonClient.getTopic(topic).publish(message);
        log.debug("Published to topic [{}]: message={}, subscribers={}", topic, message, subscribers);
        return subscribers;
    }

    /**
     * 订阅 topic，收到消息时执行 listener。
     *
     * <p>同一 key（topic + listenerKey 组合）只会注册一个监听器，
     * 重复调用不会叠加，防止重启后重复订阅。
     *
     * <pre>
     * // 订阅缓存失效通知，清除本地缓存
     * topicUtil.subscribe("cache:evict:user", Long.class, "userCacheEvict",
     *     (channel, userId) -> localCache.invalidate(userId));
     * </pre>
     *
     * @param topic       topic 名称
     * @param messageType 消息类型 Class
     * @param listenerKey 监听器唯一标识（用于去重和取消订阅）
     * @param listener    消息处理逻辑
     * @param <T>         消息类型
     */
    public <T> void subscribe(String topic, Class<T> messageType,
                              String listenerKey, MessageListener<T> listener) {
        String key = topic + ":" + listenerKey;
        // 已存在则不重复注册
        if (listenerIds.containsKey(key)) {
            log.debug("Listener [{}] already subscribed to topic [{}], skip", listenerKey, topic);
            return;
        }
        RTopic rTopic = redissonClient.getTopic(topic);
        int listenerId = rTopic.addListener(messageType, listener);
        listenerIds.put(key, listenerId);
        log.info("Subscribed to topic [{}] with listener [{}], id={}", topic, listenerKey, listenerId);
    }

    /**
     * 取消指定监听器的订阅。
     *
     * @param topic       topic 名称
     * @param listenerKey 订阅时使用的监听器唯一标识
     */
    public void unsubscribe(String topic, String listenerKey) {
        String key = topic + ":" + listenerKey;
        Integer listenerId = listenerIds.remove(key);
        if (listenerId != null) {
            redissonClient.getTopic(topic).removeListener(listenerId);
            log.info("Unsubscribed listener [{}] from topic [{}]", listenerKey, topic);
        }
    }

    /**
     * 取消该 topic 下的所有监听器。
     *
     * @param topic topic 名称
     */
    public void unsubscribeAll(String topic) {
        redissonClient.getTopic(topic).removeAllListeners();
        listenerIds.keySet().removeIf(key -> key.startsWith(topic + ":"));
        log.info("All listeners removed from topic [{}]", topic);
    }

    /**
     * 查询当前 topic 的订阅者数量。
     *
     * @param topic topic 名称
     * @return 订阅者数量
     */
    public long countSubscribers(String topic) {
        return redissonClient.getTopic(topic).countSubscribers();
    }
}
