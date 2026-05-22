package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 账号解冻集成事件（跨服务）。tag {@code account.unfrozen}。
 *
 * @author sunshixiong
 */
@Getter
public class AccountUnfrozenIntegrationEvent extends BaseEvent {

    private final Long accountId;
    private final String username;
    private final String source;
    private final Long operatorId;

    public AccountUnfrozenIntegrationEvent(Long accountId, String username, String source, Long operatorId) {
        this.accountId = accountId;
        this.username = username;
        this.source = source;
        this.operatorId = operatorId;
    }
}
