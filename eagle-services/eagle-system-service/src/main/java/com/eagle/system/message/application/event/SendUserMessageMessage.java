package com.eagle.system.message.application.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * 通用站内信发送集成事件（message 端消息契约）。
 *
 * <p>topic {@code user_message_send}。任意服务需要给用户发站内信时发布本事件，
 * 由 message 模块统一落库、推送。
 *
 * <p>与各生产方（trade-service 的 wallet / withdrawal、user-service 的 invitation）
 * 各自声明的 {@code SendUserMessageIntegrationEvent} <b>字段名对齐</b>，
 * 两侧不共享 Java 类 —— 按 {@code 02-architecture.md}「字段名是唯一契约」。
 * 对齐由 {@code contract-test} 模块在构建期校验。
 *
 * <p>本类此前是 {@code eagle-amqp-starter} 里的共享类
 * {@code com.eagle.amqp.events.SendUserMessageIntegrationEvent}，生产方与消费方
 * 直接 import 同一个 Java 类型。那样改一个字段就要发 starter、再让两个仓库同步升级，
 * 正是「字段名即契约」要避开的耦合，故拆分为两侧各自声明。
 *
 * <p><b>幂等</b>：以 {@link #bizKey} 唯一去重，同 bizKey 重复投递只落库一条。
 */
@Getter
@Setter
@NoArgsConstructor
public class SendUserMessageMessage extends BaseEvent {

    /** 接收用户 ID。 */
    private Long userId;

    /** 消息分类：{@code SYSTEM} / {@code TRADE} / {@code MARKETING} / {@code ANNOUNCEMENT}。 */
    private String category;

    /** 消息标题。 */
    private String title;

    /** 消息正文。 */
    private String content;

    /** 业务幂等键，约定 {@code "<scenario>:<aggregate-id>"}，为空时降级为 eventId 去重。 */
    private @Nullable String bizKey;
}
