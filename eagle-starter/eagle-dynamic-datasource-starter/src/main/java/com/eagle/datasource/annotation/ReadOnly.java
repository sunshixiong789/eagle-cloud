package com.eagle.datasource.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记当前方法为只读操作，自动路由到从库。
 *
 * <p>优先级低于 {@code @Transactional(readOnly = true)}，两者同时存在时效果相同。
 *
 * @author 孙士雄
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}
