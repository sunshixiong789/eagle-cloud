package com.eagle.system.message.announcement.infrastructure.push;

/**
 * 公告跨实例广播 Redis pub/sub 常量。
 *
 * @author sunshixiong
 */
public final class AnnouncementBroadcastTopics {

    /** Redis pub/sub channel：所有 system-service 实例订阅，进程内 broadcast。 */
    public static final String TOPIC = "announcement:broadcast";

    /** 订阅者唯一标识——同实例多次启动不会重复注册（{@link com.eagle.redis.util.RedissonTopicUtil} 内部去重）。 */
    public static final String LISTENER_KEY = "announcement-broadcast-listener";

    private AnnouncementBroadcastTopics() {}
}
