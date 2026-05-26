/**
 * Auth 模块的领域事件命名接口（Named Interface "event"）
 * <p>
 * 暴露跨域集成事件（{@link com.eagle.auth.core.domain.event.AccountRegisteredEvent}、
 * {@link com.eagle.auth.core.domain.event.AccountDeletedEvent}），
 * 供 system 模块订阅账号创建/删除事件并同步管理对应的 User。
 *
 * @author sunshixiong
 */
@NamedInterface("event")
package com.eagle.auth.core.domain.event;

import org.springframework.modulith.NamedInterface;
