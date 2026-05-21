package com.eagle.system.message.announcement.infrastructure.push;

import com.eagle.redis.util.RedissonTopicUtil;
import com.eagle.websocket.session.WebSocketSessionManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 在每个 system-service 实例启动时订阅 {@link AnnouncementBroadcastTopics#TOPIC}，
 * 收到广播后调用本地 {@link WebSocketSessionManager#broadcast} 推送给本进程的 STOMP 连接。
 *
 * <p>所有实例都订阅同一个 channel，Redis pub/sub 一次发布触发所有实例本地推送，
 * 共同覆盖全部在线 WebSocket 连接，无单点。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementBroadcastSubscriber {

    /** STOMP 客户端订阅路径：{@code /topic/announcements}（无需 user 前缀，全员可订阅）。 */
    public static final String DESTINATION = "/topic/announcements";

    private final RedissonTopicUtil redissonTopicUtil;
    private final WebSocketSessionManager webSocketSessionManager;

    @PostConstruct
    void subscribe() {
        redissonTopicUtil.subscribe(
                AnnouncementBroadcastTopics.TOPIC,
                AnnouncementBroadcastMessage.class,
                AnnouncementBroadcastTopics.LISTENER_KEY,
                (channel, message) -> {
                    try {
                        webSocketSessionManager.broadcast(DESTINATION, message);
                        log.debug("announcement broadcast pushed locally: id={}", message.id());
                    } catch (Exception e) {
                        log.warn("announcement local WebSocket broadcast failed: id={}", message.id(), e);
                    }
                });
        log.info("subscribed to announcement broadcast channel: {}", AnnouncementBroadcastTopics.TOPIC);
    }
}
