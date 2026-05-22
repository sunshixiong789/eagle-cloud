package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 账号冻结集成事件（跨服务）。tag {@code account.frozen}。
 *
 * <p>{@code reason} 序列化为字符串（FreezeReason 枚举名），下游按字符串解析，
 * 避免跨服务共享枚举类。
 *
 * @author sunshixiong
 */
@Getter
public class AccountFrozenIntegrationEvent extends BaseEvent {

    private final Long accountId;
    private final String username;
    private final String reason;
    private final LocalDateTime freezeUntil;
    private final Long operatorId;

    public AccountFrozenIntegrationEvent(Long accountId, String username, String reason,
                                         LocalDateTime freezeUntil, Long operatorId) {
        this.accountId = accountId;
        this.username = username;
        this.reason = reason;
        this.freezeUntil = freezeUntil;
        this.operatorId = operatorId;
    }
}
