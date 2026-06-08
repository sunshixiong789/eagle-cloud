package com.eagle.auth.core.infrastructure.event;

/**
 * auth-service 登录日志集成事件契约常量。
 */
public final class AuthLoginLogPublisher {

    public static final String TOPIC = "eagle_auth_events";
    public static final String TAG = "auth.login";

    private AuthLoginLogPublisher() {
    }
}
