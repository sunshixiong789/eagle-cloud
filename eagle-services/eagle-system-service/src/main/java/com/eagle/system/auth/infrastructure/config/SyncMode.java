package com.eagle.system.auth.infrastructure.config;

/**
 * OAuth2 客户端启动同步策略。
 *
 * @author sunshixiong
 */
public enum SyncMode {

    /**
     * 同步：yml 变更覆盖 DB 已有客户端配置（保持向后兼容行为）。
     */
    OVERWRITE,

    /**
     * 不同步：DB 已有客户端时跳过，保留运维通过 DB 做的调整。仍会创建新客户端。
     */
    CREATE_ONLY
}
