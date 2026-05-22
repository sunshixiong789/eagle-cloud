package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 账号删除集成事件（跨服务）。tag {@code account.deleted}。
 *
 * @author sunshixiong
 */
@Getter
public class AccountDeletedIntegrationEvent extends BaseEvent {

    private final Long accountId;

    public AccountDeletedIntegrationEvent(Long accountId) {
        this.accountId = accountId;
    }
}
