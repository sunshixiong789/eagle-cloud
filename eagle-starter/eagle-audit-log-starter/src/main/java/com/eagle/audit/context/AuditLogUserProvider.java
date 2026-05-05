package com.eagle.audit.context;

/**
 * 审计日志用户上下文提供接口。
 *
 * <p>业务方实现此接口以提供当前操作者信息。
 * 未实现时降级为匿名用户（anonymous）。
 *
 * @author eagle
 */
public interface AuditLogUserProvider {

    /** 当前操作者 ID，无登录上下文返回 null。 */
    String getCurrentUserId();

    /** 当前操作者名称，无登录上下文返回 null。 */
    String getCurrentUserName();

    /** 当前租户 ID，无多租户场景返回 null。 */
    default String getCurrentTenantId() {
        return null;
    }
}
