/**
 * 平台级通用集成事件契约（跨服务共享 ABI）。
 *
 * <p>这里定义所有服务都可能需要发布或订阅的"基础设施级"集成事件，
 * 与具体业务域无关。例如：
 * <ul>
 *   <li>{@link com.eagle.rocketmq.events.SendUserMessageIntegrationEvent}
 *       — 通用站内信发送事件，任意业务方可发布以触发站内信</li>
 * </ul>
 *
 * <p>放在 starter 内的考量：所有业务服务都已依赖
 * {@code eagle-rocketmq-starter}，引用契约零摩擦；同时
 * {@code message} 消费方与发布方共用同一份 schema，避免循环依赖。
 *
 * <p>修改这里的字段 = 跨服务 ABI 变更，必须保持向后兼容。
 *
 * @author 孙士雄
 */
@NullMarked
package com.eagle.rocketmq.events;

import org.jspecify.annotations.NullMarked;
