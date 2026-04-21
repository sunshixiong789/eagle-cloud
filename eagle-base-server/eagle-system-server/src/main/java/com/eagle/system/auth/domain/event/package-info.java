/**
 * Auth 模块的领域事件命名接口（Named Interface "event"）
 * <p>
 * 暴露 {@link com.eagle.auth.domain.event.AccountCreatedEvent}，
 * 供 system 模块订阅账号创建事件并自动创建对应的 User。
 *
 * @author sunshixiong
 */
@NamedInterface("event")
package com.eagle.system.auth.domain.event;

import org.springframework.modulith.NamedInterface;