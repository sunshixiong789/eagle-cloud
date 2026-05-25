package com.eagle.datapermission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略数据权限注解。
 *
 * <p>标记在方法上，使该方法绕过 {@link DataPermission} 切面的数据权限过滤，以全量数据执行查询。
 *
 * <p><b>适用场景：</b>
 * <ul>
 *   <li>超级管理员专属操作（如全局数据汇总统计）</li>
 *   <li>定时任务、批处理等内部系统调用，无需用户维度过滤</li>
 *   <li>未来支持类级 {@code @DataPermission} 时，对特定方法的排除</li>
 * </ul>
 *
 * <p><b>编程式替代方案：</b>若无法修改方法签名，可使用 {@link com.eagle.datapermission.context.DataPermissionContext}：
 * <pre>{@code
 * DataPermissionContext.ignorePermission(() -> userRepository.findAll(spec));
 * }</pre>
 *
 * @author eagle
 * @see DataPermission
 * @see com.eagle.datapermission.context.DataPermissionContext
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermissionIgnore {
}