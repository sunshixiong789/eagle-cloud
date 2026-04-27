package com.eagle.tenant;

/**
 * 多租户上下文持有者。
 *
 * <p>基于 ThreadLocal 存储当前请求的租户 ID，保证线程安全。
 *
 * @author 孙士雄
 */
public class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置当前租户 ID。
     *
     * @param tenantId 租户 ID
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * 获取当前租户 ID。
     *
     * @return 租户 ID，可能为 null
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * 清除当前租户 ID。
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
