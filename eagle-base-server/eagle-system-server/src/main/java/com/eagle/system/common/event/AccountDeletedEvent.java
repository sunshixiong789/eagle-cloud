package com.eagle.system.common.event;

/**
 * 账号删除事件（跨域事件契约）
 * <p>
 * auth 域删除 Account 后发布此事件，system 域订阅后级联删除对应的 User。
 *
 * @author sunshixiong
 */
public record AccountDeletedEvent(
        /** 被删除的账号 ID */
        Long accountId
) {
}
