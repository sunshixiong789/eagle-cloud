package com.eagle.system.auth.domain.event;

/**
 * 账号已解冻事件
 *
 * @author sunshixiong
 */
public record AccountUnfrozenEvent(
        Long accountId,
        String username,
        Source source,
        Long operatorId) {

    public enum Source {
        /** 管理员显式解冻 */
        ADMIN,
        /** 到期自动解冻 */
        AUTO
    }
}
