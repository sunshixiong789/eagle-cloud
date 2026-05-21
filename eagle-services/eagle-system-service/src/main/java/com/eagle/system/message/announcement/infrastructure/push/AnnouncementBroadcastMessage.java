package com.eagle.system.message.announcement.infrastructure.push;

import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;

import java.io.Serializable;

/**
 * 跨实例广播载体——在 Redis pub/sub 上传输的轻量消息。
 *
 * <p>实现 {@link Serializable} 是 Redisson 默认序列化的要求；fastjson2 codec
 * 也能处理 record，但保留 Serializable 保证最广兼容性。
 *
 * @author sunshixiong
 */
public record AnnouncementBroadcastMessage(
        Long id,
        AnnouncementCategory category,
        String title,
        String content
) implements Serializable {
}
