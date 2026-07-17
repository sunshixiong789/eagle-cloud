package com.eagle.auth.core.domain.port;

import java.util.Optional;

/**
 * 待绑定凭证存储端口（Driven Port，六边形架构）。
 *
 * <p>由 {@code infrastructure/adapter/RedisBindTicketStore} 通过 Redis 实现，
 * TTL 固定 10 分钟，消费即删（一次性，防重放）。
 *
 * @author sunshixiong
 */
public interface BindTicketStore {

    /**
     * 保存凭证。
     *
     * @param ticket 待绑定凭证
     * @return 高熵随机 ticketId（返回给客户端）
     */
    String save(BindTicket ticket);

    /**
     * 消费凭证：取出并删除（一次性）。
     *
     * @param ticketId ticketId
     * @return 凭证；不存在或已过期返回 empty
     */
    Optional<BindTicket> consume(String ticketId);
}
