/**
 * 站内消息模块（Message Bounded Context）。
 *
 * <p><strong>定位</strong>：平台级横切能力——通用站内消息中心。任何业务方需要给用户发站内信，
 * 通过发布 {@link com.eagle.rocketmq.events.SendUserMessageIntegrationEvent}
 * 到 {@link com.eagle.rocketmq.events.CommonMessageTopics#USER_MESSAGE_SEND}
 * 即可，本模块统一消费、落库、推送。
 *
 * <p><strong>职责</strong>：
 * <ul>
 *   <li>消费 {@code SendUserMessageIntegrationEvent}，落库 {@code user_message} 表</li>
 *   <li>提供用户侧 REST：消息列表、未读数、标记已读</li>
 *   <li>消息落库后通过 WebSocket 推送给在线用户</li>
 * </ul>
 *
 * <p><strong>解耦原则（拆分就绪）</strong>：
 * <ul>
 *   <li>{@code allowedDependencies = {}} — 不依赖任何业务模块</li>
 *   <li>不订阅任何业务集成事件，仅订阅唯一通用 topic {@code user_message_send}</li>
 *   <li>不暴露 {@code @NamedInterface} — 同进程其他模块也只能通过发布集成事件触达，
 *       未来抽出独立 message-service 时发布方零代码改动</li>
 *   <li>{@code user_message} 表不与任何其他表 JOIN，仅含 {@code user_id} 字段</li>
 *   <li>实时推送通过基础设施 starter（{@code WebSocketSessionManager}），不依赖 base 模块</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(displayName = "站内消息模块", allowedDependencies = {})
@NullMarked
package com.eagle.system.message;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
