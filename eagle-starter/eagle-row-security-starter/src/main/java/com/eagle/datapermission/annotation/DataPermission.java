package com.eagle.datapermission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解。
 *
 * <p>标记在 Service 或 Repository 方法上，切面会自动拦截第一个
 * {@link org.springframework.data.jpa.domain.Specification} 类型参数，追加当前用户的数据范围过滤条件。
 *
 * <p><b>字段名优先级：</b>注解显式设置 &gt; 全局配置 {@code eagle.data-permission.default-dept-field}。
 * 注解值为空字符串（默认）时，自动回退到全局配置默认值。
 *
 * <pre>{@code
 * // 使用全局默认字段（eagle.data-permission.default-dept-field / default-user-field）
 * @DataPermission
 * public Page<User> findUsers(Specification<User> spec, Pageable pageable) { ... }
 *
 * // 显式指定字段名，覆盖全局默认
 * @DataPermission(deptField = "department", userField = "creatorId")
 * public Page<Order> findOrders(Specification<Order> spec, Pageable pageable) { ... }
 * }</pre>
 *
 * @author eagle
 * @see DataPermissionIgnore
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 实体中对应部门 ID 的字段名。
     * 为空时使用全局配置 {@code eagle.data-permission.default-dept-field}（默认 {@code "deptId"}）。
     */
    String deptField() default "";

    /**
     * 实体中对应用户 ID 的字段名（用于 {@code SELF} 权限范围）。
     * 为空时使用全局配置 {@code eagle.data-permission.default-user-field}（默认 {@code "id"}）。
     */
    String userField() default "";
}
