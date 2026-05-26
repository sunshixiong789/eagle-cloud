package com.eagle.auth.core.infrastructure.remote;

import com.eagle.auth.core.infrastructure.event.integration.AccountRegisteredIntegrationEvent;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 跨服务同步创建 User 的内部 API 客户端
 * (调 eagle-system-service {@code POST /internal/users/from-account})。
 *
 * <p><strong>定位</strong>:RocketMQ 投递 {@code account.registered} 失败时的同步降级通道。
 * 主链路仍是 MQ —— 异步、解耦、可重试;HTTP 仅在 broker 不可达时兜底,保证不丢账号同步。
 *
 * <p>{@link AccountRegisteredIntegrationEvent} 的字段直接序列化为 JSON,
 * 与 system 端 {@code AccountRegisteredMessage} 字段对齐。
 *
 * @author sunshixiong
 */
@HttpExchange("/internal/users")
public interface SystemUserSyncClient {

    /**
     * 同步创建 User。下游已通过 {@code UserRepository.existsByAccountId} + 唯一索引兜住幂等,
     * 重复调用安全。
     */
    @PostExchange("/from-account")
    void syncFromAccount(@RequestBody AccountRegisteredIntegrationEvent event);
}
