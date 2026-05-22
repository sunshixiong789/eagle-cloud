package com.eagle.system.base.infrastructure.messaging.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 账号注册集成事件(base 端 RocketMQ 消息契约)。
 * <p>
 * 与 auth-service 端 {@code com.eagle.auth.infrastructure.event.integration
 * .AccountRegisteredIntegrationEvent} 字段对齐;两侧各自维护,新增字段保持向后兼容。
 * <p>
 * topic {@code eagle.auth.events},tag {@code account.registered}。
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountRegisteredMessage extends BaseEvent {

    private Long accountId;
    private String username;
    private String phone;
    private String nickname;
    private String avatar;
    private String email;
}
