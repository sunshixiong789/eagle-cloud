package com.eagle.amqp.events;

/**
 * 平台级通用集成事件 Topic 常量。
 *
 * <p>命名遵循 {@code {env}_{domain}_{event}} 规范——此处常量为 logical 名（去掉 env 前缀），
 * Producer/Consumer 调用时由 {@link com.eagle.amqp.properties.AmqpProperties#getExchangePrefix()}
 * 自动拼接（如 {@code dev_}）。
 *
 * @author eagle
 */
public final class CommonMessageTopics {

    /**
     * 通用站内信发送 Topic。
     *
     * <p>任意业务方发布 {@link SendUserMessageIntegrationEvent} 到该 Topic，
     * 由 {@code eagle-system-service} 的 message 模块订阅消费、落库、推送。
     *
     * <p>实际 exchange 名 = {@code exchange-prefix + USER_MESSAGE_SEND}，例如 {@code dev_user_message_send}。
     */
    public static final String USER_MESSAGE_SEND = "user_message_send";

    private CommonMessageTopics() {}
}
