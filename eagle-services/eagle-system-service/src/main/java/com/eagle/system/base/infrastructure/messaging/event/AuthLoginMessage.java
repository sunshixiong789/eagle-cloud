package com.eagle.system.base.infrastructure.messaging.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * auth-service 登录日志集成事件(system-service 消费方本地契约)。
 */
@Getter
@Setter
@NoArgsConstructor
public class AuthLoginMessage extends BaseEvent {

    private String eventVersion;
    private Long accountId;
    private String username;
    private String ip;
    private String userAgent;
    private boolean success;
    private String failReason;
}
