package com.eagle.sentinel.annotation;

/**
 * Sentinel 流控行为枚举。
 *
 * <p>对应 Sentinel {@code RuleConstant} 中定义的控制行为常量：
 * <ul>
 *   <li>{@link #FAST_FAIL} → {@code RuleConstant.CONTROL_BEHAVIOR_DEFAULT}（默认，超过阈值直接拒绝）</li>
 *   <li>{@link #WARM_UP} → {@code RuleConstant.CONTROL_BEHAVIOR_WARM_UP}（预热/冷启动）</li>
 *   <li>{@link #RATE_LIMITER} → {@code RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER}（匀速排队）</li>
 * </ul>
 *
 * @author 孙士雄
 * @see RateLimit
 */
public enum FlowControlBehavior {

    /**
     * 快速失败（默认行为）。
     *
     * <p>当 QPS 超过阈值时，直接抛出 {@code FlowException}，快速拒绝请求。
     * 适用于对延迟敏感、不希望请求堆积的场景。
     */
    FAST_FAIL,

    /**
     * 预热模式（Warm Up / 冷启动）。
     *
     * <p>系统启动初期，流量阈值从低值逐步升高到设定值，
     * 避免冷启动时突增流量把系统压垮。预热时长由 {@link RateLimit#warmUpPeriodSec()} 控制。
     */
    WARM_UP,

    /**
     * 匀速排队模式（漏桶算法）。
     *
     * <p>以固定速率匀速通过请求，多余的请求排队等待，超过最大等待时间则拒绝。
     * 最大等待时间由 {@link RateLimit#maxQueueingTimeMs()} 控制。
     * 适用于需要削峰填谷的场景。
     */
    RATE_LIMITER
}
