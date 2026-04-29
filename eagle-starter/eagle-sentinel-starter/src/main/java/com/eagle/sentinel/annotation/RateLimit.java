package com.eagle.sentinel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sentinel 声明式限流注解。
 *
 * <p>标注在方法或类上，由 {@link com.eagle.sentinel.aspect.RateLimitAspect} 拦截并动态注册
 * Sentinel 流控规则。标注在类上时，类中所有 public 方法均受限流保护。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 方法级：每秒最多 50 次请求，超过快速失败
 * @RateLimit(resource = "createOrder", qps = 50)
 * public OrderResponse createOrder(CreateOrderRequest request) { ... }
 *
 * // 类级：类内所有方法每秒最多 100 次，预热 20 秒
 * @RateLimit(qps = 100, behavior = FlowControlBehavior.WARM_UP, warmUpPeriodSec = 20)
 * @RestController
 * public class OrderController { ... }
 *
 * // 匀速排队，最长等待 1000ms
 * @RateLimit(qps = 10, behavior = FlowControlBehavior.RATE_LIMITER, maxQueueingTimeMs = 1000)
 * public void sendSms(String phone) { ... }
 * }</pre>
 *
 * @author 孙士雄
 * @see com.eagle.sentinel.aspect.RateLimitAspect
 * @see FlowControlBehavior
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 资源名称。
     *
     * <p>默认为空，此时资源名自动取 {@code 类名.方法名}（如 {@code OrderController.createOrder}）。
     * 显式指定可跨类共享同一限流计数器。
     *
     * @return 资源名，默认空字符串
     */
    String resource() default "";

    /**
     * 每秒允许通过的最大请求数（QPS 阈值）。
     *
     * @return QPS 阈值，默认 100
     */
    double qps() default 100;

    /**
     * 并发线程数限制。
     *
     * <p>当值大于 0 时，同时限制 QPS 和并发线程数两个维度；
     * 为 0 则仅限制 QPS。
     *
     * @return 最大并发线程数，0 表示不限制，默认 0
     */
    int threads() default 0;

    /**
     * 流控行为，超过阈值时的处理策略。
     *
     * @return 流控行为，默认 {@link FlowControlBehavior#FAST_FAIL}
     */
    FlowControlBehavior behavior() default FlowControlBehavior.FAST_FAIL;

    /**
     * 预热时长（秒）。
     *
     * <p>仅在 {@link #behavior()} 为 {@link FlowControlBehavior#WARM_UP} 时有效。
     * 系统启动后在此时间内将阈值从低值渐进提升至 {@link #qps()} 设定值。
     *
     * @return 预热时长（秒），默认 10
     */
    int warmUpPeriodSec() default 10;

    /**
     * 最大排队等待时间（毫秒）。
     *
     * <p>仅在 {@link #behavior()} 为 {@link FlowControlBehavior#RATE_LIMITER} 时有效。
     * 超过此等待时间的请求直接丢弃。
     *
     * @return 最大排队等待时间（ms），默认 500
     */
    int maxQueueingTimeMs() default 500;
}
