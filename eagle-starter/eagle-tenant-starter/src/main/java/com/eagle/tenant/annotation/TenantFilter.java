package com.eagle.tenant.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 租户过滤器注解。
 *
 * <p>标记在 Service 或 Repository 方法上，触发 Hibernate Filter 自动注入当前租户 ID。
 * 要求对应实体已定义 {@code @FilterDef(name = "tenantFilter")} 和 {@code @Filter(name = "tenantFilter")}。
 *
 * @author eagle
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantFilter {
}
