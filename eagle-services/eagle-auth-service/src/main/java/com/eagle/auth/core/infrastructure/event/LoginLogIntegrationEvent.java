package com.eagle.auth.core.infrastructure.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 登录日志集成事件。
 *
 * <p>tag {@code auth.login}，topic {@code eagle_auth_events}。
 */
@Getter
@NoArgsConstructor
public class LoginLogIntegrationEvent extends BaseEvent {

    /** 事件协议版本。 */
    public static final String EVENT_VERSION = "1.0";

    private String eventVersion;
    private Long accountId;
    private String username;
    private String ip;
    private String userAgent;
    private boolean success;
    private String failReason;

    public LoginLogIntegrationEvent(Long accountId, String username, String ip,
                                    String userAgent, boolean success, String failReason) {
        this.eventVersion = EVENT_VERSION;
        this.accountId = accountId;
        this.username = username;
        this.ip = ip;
        this.userAgent = userAgent;
        this.success = success;
        this.failReason = failReason;
    }
}
