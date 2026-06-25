package com.eagle.auth.core.domain.event;

/**
 * 手机号变更事件（跨域集成事件）。
 *
 * <p>auth 域替换 Account 手机号后发布，由 {@code AuthIntegrationEventPublisher} 转成
 * 集成事件发往 RocketMQ。事件类放在 domain/event 包，通过 Named Interface "event" 暴露。
 *
 * @author sunshixiong
 */
public record AccountPhoneChangedEvent(
        // 账号 ID
        Long accountId,

        // 变更后的新手机号
        String phone
) {
}
