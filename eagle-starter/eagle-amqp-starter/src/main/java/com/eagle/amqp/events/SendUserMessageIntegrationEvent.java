package com.eagle.amqp.events;

import com.eagle.common.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * 通用站内信发送集成事件。
 *
 * <p>任意业务方需要给用户发送站内信时，发布本事件到
 * {@link CommonMessageTopics#USER_MESSAGE_SEND}，由 message 模块统一消费、落库、推送。
 *
 * <p><strong>幂等保障</strong>：消费方以 {@link #bizKey} 作为唯一键去重——
 * 业务发布方应对每条逻辑消息使用稳定 key（如 {@code "rebate-credited:" + orderId}），
 * 同一 bizKey 重复投递只落库一条。bizKey 为 {@code null} 时降级为 eventId 去重，
 * 但跨场景重复风险更高，因此**强烈建议传入有业务语义的 bizKey**。
 *
 * <p><strong>schema 稳定性</strong>：本类是跨服务 ABI，新增字段必须有默认值；
 * 删除/重命名字段属破坏性变更，需协调所有发布方/消费方同步发布。
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
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendUserMessageIntegrationEvent extends BaseEvent {

    /**
     * 接收用户 ID（必填）。
     */
    private Long userId;

    /**
     * 消息分类（必填）。建议取值：{@code SYSTEM} / {@code TRADE} / {@code MARKETING} / {@code ANNOUNCEMENT}。
     *
     * <p>使用字符串而非枚举：消费方可自由扩展枚举值，发布方无需升级 SDK。
     */
    private String category;

    /**
     * 消息标题（必填）。
     */
    private String title;

    /**
     * 消息正文（必填，纯文本或受信任的 HTML，由消费方/前端决定渲染策略）。
     */
    private String content;

    /**
     * 业务幂等键（强烈建议）。
     *
     * <p>消费方按本字段唯一去重——同一 bizKey 重复投递不会重复落库。
     * 命名约定：{@code "<scenario>:<aggregate-id>"}，例如 {@code "rebate-credited:1001"}。
     */
    @Nullable
    private String bizKey;
}
