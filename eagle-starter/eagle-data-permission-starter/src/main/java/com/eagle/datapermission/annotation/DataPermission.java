package com.eagle.datapermission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解。
 *
 * <p>标记在 Service 或 Repository 方法上，表示该方法需要进行数据权限过滤。
 * 具体过滤逻辑由 {@link com.eagle.datapermission.provider.DataPermissionProvider} 提供。
 *
 * @author 孙士雄
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 实体类中对应部门 ID 的字段名，默认 {@code "deptId"}。
     */
    String deptField() default "deptId";

    /**
     * 实体类中对应用户 ID 的字段名，默认 {@code "id"}（用于 SELF 范围）。
     */
    String userField() default "id";
}
