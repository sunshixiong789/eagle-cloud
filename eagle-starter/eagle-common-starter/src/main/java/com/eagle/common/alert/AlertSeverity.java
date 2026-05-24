package com.eagle.common.alert;

/**
 * 告警严重程度分级。
 *
 * <p>Logback 侧通过 MDC 字段 {@code alert.severity} 在 prod 环境的 webhook appender 中过滤,
 * 例如只把 {@link #ERROR} 与 {@link #CRITICAL} 转钉钉/企微,避免低优先级噪声。
 *
 * @author sunshixiong
 */
public enum AlertSeverity {

    /** 信息性事件,例如启动 / 关闭 / 配置变更。 */
    INFO,

    /** 警告性事件,例如降级触发 / 重试达到阈值。 */
    WARN,

    /** 业务失败事件,例如 DLQ 投递 / 跨服务调用持续失败。 */
    ERROR,

    /** 关键事件,要求 oncall 立即介入,例如核心链路阻塞 / 数据一致性破坏。 */
    CRITICAL
}
