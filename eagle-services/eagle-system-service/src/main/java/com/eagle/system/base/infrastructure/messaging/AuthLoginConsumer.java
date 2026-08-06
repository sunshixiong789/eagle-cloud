package com.eagle.system.base.infrastructure.messaging;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.system.base.application.service.SystemLogRecorder;
import com.eagle.system.base.infrastructure.messaging.event.AuthLoginMessage;
import org.springframework.stereotype.Component;

/**
 * 消费 auth-service 发布的登录日志事件，写入 system-service 系统日志表。
 */
@Component
public class AuthLoginConsumer extends AbstractAmqpListener<AuthLoginMessage> {

    static final String TOPIC = "eagle_auth_events";
    static final String TAG = "auth.login";
    static final String CONSUMER_GROUP = "system_auth_login";

    private final SystemLogRecorder recorder;

    public AuthLoginConsumer(AmqpProperties props, SystemLogRecorder recorder) {
        super(props);
        this.recorder = recorder;
    }

    @Override
    protected String getTopic() {
        return TOPIC;
    }

    @Override
    protected Class<AuthLoginMessage> getEventClass() {
        return AuthLoginMessage.class;
    }

    @Override
    protected String getConsumerGroup() {
        return CONSUMER_GROUP;
    }

    @Override
    protected String getRoutingKey() {
        return TAG;
    }

    @Override
    protected void handle(AuthLoginMessage event) {
        recorder.recordLogin(event);
    }
}
