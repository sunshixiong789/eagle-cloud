package com.eagle.websocket.offline;

import java.time.Duration;
import java.util.List;

/**
 * 离线消息存储接口。
 *
 * <p>用户不在线时，将消息持久化存储，用户重连后自动推送。
 * 默认实现基于 Redis（{@link RedisOfflineMessageStore}），
 * 业务方可提供自定义实现（如数据库存储）覆盖默认 Bean。
 *
 * <p>使用场景：
 * <ul>
 *   <li>推送消息时检测用户是否在线，不在线则调用 {@link #store} 存储</li>
 *   <li>用户重连成功后，调用 {@link #getAndClear} 取出离线消息批量推送</li>
 * </ul>
 *
 * @author eagle
 */
public interface OfflineMessageStore {

    /**
     * 存储离线消息。
     *
     * <p>同一用户可存储多条消息，按存储顺序（先进先出）排列。
     * 超过 {@code ttl} 后消息自动过期，防止无限积累。
     *
     * @param userId  目标用户 ID
     * @param message 消息内容（建议使用 JSON 字符串以保持格式统一）
     * @param ttl     消息存活时长（超时后自动删除）
     */
    void store(String userId, String message, Duration ttl);

    /**
     * 获取并清除指定用户的所有离线消息。
     *
     * <p>此操作应保证原子性：取出后即删除，避免重复推送。
     * 若取出过程中发生异常，建议实现方回滚（如数据库事务、Lua 脚本）。
     *
     * @param userId 目标用户 ID
     * @return 消息列表，按存储顺序排列；用户无离线消息时返回空列表
     */
    List<String> getAndClear(String userId);

    /**
     * 查询离线消息数量（不消费消息）。
     *
     * <p>用于在推送前判断是否有积压消息，避免不必要的取出操作。
     *
     * @param userId 目标用户 ID
     * @return 离线消息数量，用户不存在或无消息时返回 0
     */
    long count(String userId);
}
