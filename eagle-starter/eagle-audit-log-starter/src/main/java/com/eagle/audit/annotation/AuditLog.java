package com.eagle.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志注解。
 *
 * <p>标注在 Service / Controller 方法上，切面自动记录操作信息：
 * <pre>
 * &#64;AuditLog(module = "订单管理", action = "创建订单")
 * public OrderResponse createOrder(CreateOrderRequest request) { ... }
 * </pre>
 *
 * @author eagle
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作所属模块。
     */
    String module() default "";

    /**
     * 操作描述，支持 Spring EL（引用方法参数）。
     */
    String action() default "";

    /**
     * 是否记录请求参数（敏感接口设为 false）。
     */
    boolean logArgs() default true;

    /**
     * 是否记录返回结果。
     */
    boolean logResult() default false;
}
