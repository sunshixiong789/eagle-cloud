package com.eagle.datasource.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记当前方法或类为只读操作，自动路由到从库。
 *
 * <p>类级注解表示该类所有方法均路由到从库；方法级注解优先级更高，可覆盖类级行为。
 * 与 {@code @Transactional(readOnly = true)} 同时使用时效果等同，无需重复声明。
 *
 * @author 孙士雄
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}
