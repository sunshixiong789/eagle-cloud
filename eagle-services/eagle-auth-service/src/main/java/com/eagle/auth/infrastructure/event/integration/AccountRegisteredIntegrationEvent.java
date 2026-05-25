package com.eagle.auth.infrastructure.event.integration;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 账号注册集成事件（跨服务）。
 *
 * <p>tag {@code account.registered}，topic {@code eagle_auth_events}。
 *
 * <p><strong>版本契约</strong>:消费方按 {@link #eventVersion} 区分协议版本。
 * 字段加减(向后兼容)<em>不</em>升版本——消费方 JSON 反序列化天然容忍未知/缺失字段;
 * 仅当字段类型变更、字段语义变更或字段重命名等不兼容修改时,才需升 {@code eventVersion}
 * 并在过渡期双发新旧版本(参见 rules/29-event-driven.md)。
 *
 * @author sunshixiong
 */
@Getter
public class AccountRegisteredIntegrationEvent extends BaseEvent {

    /** 事件协议版本,破坏性变更时升此值。 */
    public static final String EVENT_VERSION = "1.0";

    private final String eventVersion;
    private final Long accountId;
    private final String username;
    private final String phone;
    private final String nickname;
    private final String avatar;
    private final String email;

    public AccountRegisteredIntegrationEvent(Long accountId, String username, String phone,
                                             String nickname, String avatar, String email) {
        this.eventVersion = EVENT_VERSION;
        this.accountId = accountId;
        this.username = username;
        this.phone = phone;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
    }
}
