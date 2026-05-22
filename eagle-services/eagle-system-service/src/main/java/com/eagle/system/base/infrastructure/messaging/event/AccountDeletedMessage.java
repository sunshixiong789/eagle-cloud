package com.eagle.system.base.infrastructure.messaging.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 账号删除集成事件(base 端 RocketMQ 消息契约)。
 * <p>
 * 与 auth-service 端 {@code com.eagle.auth.infrastructure.event.integration
 * .AccountDeletedIntegrationEvent} 字段对齐。
 * <p>
 * topic {@code eagle.auth.events},tag {@code account.deleted}。
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletedMessage extends BaseEvent {

    private Long accountId;
}
