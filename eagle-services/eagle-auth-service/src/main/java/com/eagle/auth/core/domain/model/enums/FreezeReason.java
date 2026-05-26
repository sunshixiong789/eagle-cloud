package com.eagle.auth.core.domain.model.enums;

/**
 * 账号冻结原因枚举
 *
 * @author sunshixiong
 */
public enum FreezeReason {
    /**
     * 管理员手动冻结
     */
    ADMIN,
    /**
     * 风控触发（预留）
     */
    RISK_CONTROL,
    /**
     * 其他（兼容旧 locked 迁移）
     */
    OTHER
}
