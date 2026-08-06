package com.eagle.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式限流注解（Resilience4J 实现）。
 *
 * <p>标注在方法或类上，由 {@link com.eagle.resilience.aspect.RateLimitAspect} 拦截，
 * 首次调用时按注解配置创建 Resilience4J {@code RateLimiter}（以及可选的 {@code Bulkhead}）。
 * 标注在类上时，类中所有 public 方法均受限流保护。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 方法级：每秒最多 50 次请求，超过快速失败
 * @RateLimit(resource = "createOrder", qps = 50)
 * public OrderResponse createOrder(CreateOrderRequest request) { ... }
 *
 * // 排队等待，最长等待 1000ms
 * @RateLimit(qps = 10, behavior = RateLimitBehavior.QUEUEING, maxQueueingTimeMs = 1000)
 * public void sendSms(String phone) { ... }
 *
 * // 同时限制 QPS 与并发数
 * @RateLimit(qps = 100, threads = 20)
 * public void heavyCall() { ... }
 * }</pre>
 *
 * <p>触发限流时抛出 Resilience4J 的 {@code RequestNotPermitted}（并发超限时为
 * {@code BulkheadFullException}），由 {@link com.eagle.resilience.handler.RateLimitExceptionHandler}
 * 统一转成 HTTP 429。
 *
 * <p><b>迁移说明</b>：本注解取代原 {@code com.eagle.sentinel.annotation.RateLimit}，
 * 属性名与语义保持一致，仅 {@code behavior} 的枚举类型换为 {@link RateLimitBehavior}
 * （不再提供 {@code WARM_UP}，原因见该枚举文档）。
 *
 * <p><b>限流是单实例维度的</b>：每个进程各自计数。多副本部署时集群总阈值 = {@code qps} × 副本数，
 * 弹性伸缩会让总阈值漂移；需要集群级限流请在网关层实现。
 *
 * @author eagle
 * @see com.eagle.resilience.aspect.RateLimitAspect
 * @see RateLimitBehavior
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 资源名称。
     *
     * <p>默认为空，此时资源名自动取 {@code 简单类名.方法名}（如 {@code OrderController.createOrder}）。
     * 显式指定可跨类共享同一限流计数器。
     *
     * @return 资源名，默认空字符串
     */
    String resource() default "";

    /**
     * 每秒允许通过的最大请求数（QPS 阈值）。
     *
     * <p>映射为 Resilience4J {@code limitForPeriod}，刷新周期固定 1 秒。
     * 小数会向下取整（Resilience4J 的令牌数为整型）。
     *
     * @return QPS 阈值，默认 100
     */
    double qps() default 100;

    /**
     * 并发调用数限制。
     *
     * <p>大于 0 时额外套一层 Resilience4J {@code Bulkhead}（信号量隔离），
     * 同时限制 QPS 和并发两个维度；为 0 则仅限制 QPS。
     *
     * @return 最大并发调用数，0 表示不限制，默认 0
     */
    int threads() default 0;

    /**
     * 限流行为，令牌耗尽时的处理策略。
     *
     * @return 限流行为，默认 {@link RateLimitBehavior#FAST_FAIL}
     */
    RateLimitBehavior behavior() default RateLimitBehavior.FAST_FAIL;

    /**
     * 最大排队等待时间（毫秒）。
     *
     * <p>仅在 {@link #behavior()} 为 {@link RateLimitBehavior#QUEUEING} 时有效。
     * 超过此等待时间仍未拿到令牌的请求被拒绝。
     *
     * @return 最大排队等待时间（ms），默认 500
     */
    int maxQueueingTimeMs() default 500;
}
