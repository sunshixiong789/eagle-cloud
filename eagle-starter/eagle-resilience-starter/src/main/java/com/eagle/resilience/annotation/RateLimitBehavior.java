package com.eagle.resilience.annotation;

/**
 * 限流行为枚举（Resilience4J 语义）。
 *
 * <p>对应 Resilience4J {@code RateLimiterConfig.timeoutDuration}：
 * <ul>
 *   <li>{@link #FAST_FAIL} → {@code timeoutDuration = 0}，拿不到令牌立即拒绝</li>
 *   <li>{@link #QUEUEING} → {@code timeoutDuration = maxQueueingTimeMs}，等待令牌，超时才拒绝</li>
 * </ul>
 *
 * <p><b>迁移说明</b>：本枚举取代原 {@code com.eagle.sentinel.annotation.FlowControlBehavior}。
 * 原有的 {@code WARM_UP}（冷启动预热）在 Resilience4J 中<b>没有等价实现</b>，故此处不提供 ——
 * 这是刻意为之：静默降级成 {@code FAST_FAIL} 会造成隐蔽的行为变更，
 * 移除后任何遗留用法都会变成编译错误，由人显式决策。
 * 确需预热能力时应在网关层实现，或改用逐步放开阈值的配置化方案。
 *
 * @author eagle
 * @see RateLimit
 */
public enum RateLimitBehavior {

    /**
     * 快速失败（默认行为）。
     *
     * <p>当前周期令牌耗尽时立即抛出 {@code RequestNotPermitted}，不等待。
     * 适用于对延迟敏感、不希望请求堆积的场景。
     */
    FAST_FAIL,

    /**
     * 排队等待。
     *
     * <p>令牌耗尽时最多等待 {@link RateLimit#maxQueueingTimeMs()} 毫秒，
     * 期间拿到令牌则放行，超时则拒绝。适用于需要削峰填谷的场景。
     *
     * <p>注意：等待会占用调用线程。虚拟线程下开销可接受，
     * 平台线程池下需评估等待时长对线程池的挤占。
     */
    QUEUEING
}
