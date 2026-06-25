package com.eagle.auth.core.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 手机号变更集成事件（跨服务）。tag {@code account.phone-changed}，topic {@code eagle_auth_events}。
 *
 * <p>版本契约见 {@link AccountRegisteredIntegrationEvent}。当前无消费方，预留给未来需要
 * phone 副本/通知的服务。
 *
 * @author sunshixiong
 */
@Getter
public class AccountPhoneChangedIntegrationEvent extends BaseEvent {

    /** 事件协议版本,破坏性变更时升此值。 */
    public static final String EVENT_VERSION = "1.0";

    private final String eventVersion;
    private final Long accountId;
    private final String phone;

    public AccountPhoneChangedIntegrationEvent(Long accountId, String phone) {
        this.eventVersion = EVENT_VERSION;
        this.accountId = accountId;
        this.phone = phone;
    }
}
