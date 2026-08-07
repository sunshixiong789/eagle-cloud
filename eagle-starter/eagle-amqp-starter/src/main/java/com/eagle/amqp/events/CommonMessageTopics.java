package com.eagle.amqp.events;

/**
 * 平台级通用集成事件 Topic 常量。
 *
 * <p>命名遵循 {@code {env}_{domain}_{event}} 规范——此处常量为 logical 名（去掉 env 前缀），
 * Producer/Consumer 调用时由 {@link com.eagle.amqp.properties.AmqpProperties#getExchangePrefix()}
 * 自动拼接（如 {@code dev_}）。
 *

 * <p><b>⚠️ 已废弃</b>：本类是跨服务共享的集成事件载荷，违反
 * {@code 02-architecture.md}「集成事件契约：字段名是唯一契约」——
 * 生产方与消费方必须各自声明载荷类，禁止共享 Java 类型或抽 shared-events.jar。
 * 共享的实际代价：改一个字段要发 starter 到 Nexus，再让 ease-mind-servers 与
 * eagle-cloud 两个仓库同步升级。
 *
 * <p>各方已改为自行声明：trade-service 的 {@code wallet} / {@code withdrawal}、
 * user-service 的 {@code invitation} 各有一份 {@code SendUserMessageIntegrationEvent}，
 * 消费方 eagle-system-service 为 {@code SendUserMessageMessage}，
 * 字段对齐由 {@code contract-test} 模块校验。本类下个版本移除。
 *
 * @author eagle
 * @deprecated 跨服务共享载荷/常量，改由各方自行声明，下个版本移除。
 */
@Deprecated(since = "1.6.0", forRemoval = true)
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
