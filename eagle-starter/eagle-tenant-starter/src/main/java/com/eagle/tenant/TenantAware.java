package com.eagle.tenant;

/**
 * 租户感知接口。
 *
 * <p>实体实现此接口表示需要租户隔离。COLUMN 模式下实体需自行添加：
 * <pre>
 * &#64;FilterDef(name = "tenantFilter", parameters = &#64;ParamDef(name = "tenantId", type = String.class))
 * &#64;Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
 * </pre>
 *
 * @author 孙士雄
 */
public interface TenantAware {

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID
     */
    String getTenantId();

    /**
     * 设置租户 ID。
     *
     * @param tenantId 租户 ID
     */
    void setTenantId(String tenantId);
}
