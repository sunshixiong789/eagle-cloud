package com.eagle.websocket.offline;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis List 的离线消息存储实现。
 *
 * <p>key 格式：{@code eagle:ws:offline:{userId}}
 *
 * <p>存储策略：
 * <ul>
 *   <li>写入使用 {@code LPUSH}（追加到列表末尾，Redisson List.add()），支持多消息顺序存储</li>
 *   <li>读取使用 {@code readAll()} + {@code delete()} 保证原子性取出并清除</li>
 *   <li>每次写入后刷新 TTL，避免长期不活跃用户的消息永久占用内存</li>
 * </ul>
 *
 * <p>注意：{@code getAndClear} 的 readAll + delete 操作在极端并发场景下
 * 可能存在竞态，如需严格原子性可改用 Lua 脚本实现。
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class RedisOfflineMessageStore implements OfflineMessageStore {

    /**
     * Redis key 前缀
     */
    private static final String KEY_PREFIX = "eagle:ws:offline:";

    private final RedissonClient redissonClient;

    /**
     * {@inheritDoc}
     *
     * <p>使用 Redisson {@link RList#add} 追加消息，并刷新整个 List 的过期时间。
     */
    @Override
    public void store(String userId, String message, Duration ttl) {
        RList<String> list = redissonClient.getList(KEY_PREFIX + userId);
        list.add(message);
        list.expire(ttl);
    }

    /**
     * {@inheritDoc}
     *
     * <p>先 {@link RList#readAll()} 读取全部消息，再 {@link RList#delete()} 删除 key。
     * 用户无离线消息时返回空列表。
     */
    @Override
    public List<String> getAndClear(String userId) {
        RList<String> list = redissonClient.getList(KEY_PREFIX + userId);
        if (!list.isExists()) {
            return Collections.emptyList();
        }
        List<String> messages = new ArrayList<>(list.readAll());
        list.delete();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用 {@link RList#size()} 查询消息数量，key 不存在时返回 0。
     */
    @Override
    public long count(String userId) {
        RList<String> list = redissonClient.getList(KEY_PREFIX + userId);
        if (!list.isExists()) {
            return 0L;
        }
        return list.size();
    }
}
