package com.eagle.websocket.offline;

import java.time.Instant;

/**
 * 离线消息数据结构。
 *
 * @param userId      目标用户 ID
 * @param destination 消息目标路径
 * @param payload     已序列化的 JSON 消息体
 * @param storedAt    消息存储时间
 * @author eagle
 */
public record OfflineMessage(
        String userId,
        String destination,
        String payload,
        Instant storedAt) {

    /**
     * 快捷工厂方法，存储时间自动设为当前时刻。
     */
    public static OfflineMessage of(String userId, String destination, String payload) {
        return new OfflineMessage(userId, destination, payload, Instant.now());
    }
}
