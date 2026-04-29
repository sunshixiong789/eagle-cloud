package com.eagle.idempotency.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解。
 *
 * <p>标注在 Controller 方法上，防止重复请求（如重复下单、重复支付）。
 * 支持两种幂等模式：
 * <ul>
 *   <li>{@link IdempotencyMode#TOKEN}：客户端预先申请一次性 Token，请求时通过 Header 携带</li>
 *   <li>{@link IdempotencyMode#BUSINESS_KEY}：基于业务键（SpEL 表达式）防重</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // TOKEN 模式（默认）
 * @PostMapping("/orders")
 * @Idempotent
 * public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) { ... }
 *
 * // BUSINESS_KEY 模式
 * @PostMapping("/payments")
 * @Idempotent(mode = IdempotencyMode.BUSINESS_KEY, key = "#request.orderNo")
 * public PaymentResponse pay(@Valid @RequestBody PayRequest request) { ... }
 * }</pre>
 *
 * @author sunshixiong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等模式，默认 {@link IdempotencyMode#TOKEN}。
     */
    IdempotencyMode mode() default IdempotencyMode.TOKEN;

    /**
     * BUSINESS_KEY 模式时，SpEL 表达式提取业务键。
     * <p>示例：{@code "#request.orderNo"}、{@code "#p0.tradeNo"}
     * <p>TOKEN 模式下此字段忽略。
     */
    String key() default "";

    /**
     * TOKEN 模式下从请求中读取 Token 的 Header 名称，默认 {@code X-Idempotency-Token}。
     */
    String tokenHeader() default "X-Idempotency-Token";

    /**
     * 幂等校验失败时的提示消息（i18n key 或直接消息文本）。
     */
    String message() default "重复请求，请勿重复提交";

    /**
     * 自定义键提取器 Bean 名称（BUSINESS_KEY 模式下，优先于 {@link #key()} SpEL 表达式）。
     * <p>对应 Bean 须实现 {@link com.eagle.idempotency.extractor.IdempotencyKeyExtractor} 接口。
     * <p>当需要复杂的键提取逻辑（例如根据请求头 + 请求体联合计算键）时，
     * 通过实现该接口并注册为 Spring Bean，在此处填写 Bean 名称即可。
     */
    String keyExtractor() default "";
}
