package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 账号删除集成事件（跨服务）。tag {@code account.deleted}。
 *
 * <p>版本契约见 {@link AccountRegisteredIntegrationEvent}。
 *
 * @author sunshixiong
 */
@Getter
public class AccountDeletedIntegrationEvent extends BaseEvent {

    /** 事件协议版本,破坏性变更时升此值。 */
    public static final String EVENT_VERSION = "1.0";

    private final String eventVersion;
    private final Long accountId;

    public AccountDeletedIntegrationEvent(Long accountId) {
        this.eventVersion = EVENT_VERSION;
        this.accountId = accountId;
    }
}
