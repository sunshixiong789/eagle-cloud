package com.eagle.system.message.announcement.infrastructure.push;

import com.eagle.redis.util.RedissonTopicUtil;
import com.eagle.system.message.announcement.domain.event.AnnouncementPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 监听 {@link AnnouncementPublishedEvent}（事务提交后），将公告发到
 * Redis pub/sub channel {@link AnnouncementBroadcastTopics#TOPIC}，
 * 触发所有 system-service 实例本地 WebSocket 广播。
 *
 * <p>不直接在本进程 broadcast：本进程只覆盖本实例的 STOMP 连接，
 * 多实例部署时其它实例的 connection 拿不到消息——所以必须经 Redis pub/sub 同步。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementBroadcastPublisher {

    private final RedissonTopicUtil redissonTopicUtil;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnnouncementPublished(AnnouncementPublishedEvent event) {
        AnnouncementBroadcastMessage payload = new AnnouncementBroadcastMessage(
                event.getAnnouncementId(), event.getCategory(),
                event.getTitle(), event.getContent());
        long subscribers = redissonTopicUtil.publish(AnnouncementBroadcastTopics.TOPIC, payload);
        log.info("announcement broadcast dispatched: id={}, subscribers={}",
                event.getAnnouncementId(), subscribers);
    }
}
