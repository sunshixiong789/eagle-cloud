package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 账号注册集成事件（跨服务）。
 *
 * <p>tag {@code account.registered}，topic {@code eagle.auth.events}。
 *
 * @author sunshixiong
 */
@Getter
public class AccountRegisteredIntegrationEvent extends BaseEvent {

    private final Long accountId;
    private final String username;
    private final String phone;
    private final String nickname;
    private final String avatar;
    private final String email;

    public AccountRegisteredIntegrationEvent(Long accountId, String username, String phone,
                                             String nickname, String avatar, String email) {
        this.accountId = accountId;
        this.username = username;
        this.phone = phone;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
    }
}
