package com.eagle.auth.domain.event;

/**
 * 账号删除事件（跨域集成事件）
 * <p>
 * auth 域删除 Account 后发布此事件，system 域订阅后级联删除对应的 User。
 * 事件类放在 auth 域的 domain/event 包中，通过 Named Interface "event" 暴露给其他模块。
 *
 * @author sunshixiong
 */
public record AccountDeletedEvent(
        // 被删除的账号 ID
        Long accountId
) {
}
