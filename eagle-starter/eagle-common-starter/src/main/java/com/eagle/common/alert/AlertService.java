package com.eagle.common.alert;

/**
 * 告警发送 SPI。
 *
 * <p>默认实现 {@link LoggingAlertService} 仅打结构化 ERROR 日志,
 * 真实 webhook (钉钉 / 企微 / 飞书 / 邮件) 由 Logback Appender 配置侧接管 ——
 * 业务代码不耦合具体推送通道,只声明"这是一条告警"。
 *
 * <p>需要直接对接 IM 的项目,可在自己的 starter 或服务模块中提供
 * {@code @ConditionalOnMissingBean} 的 {@code AlertService} 覆盖实现。
 *
 * <p>使用方式:
 * <pre>{@code
 * alertService.send(AlertEvent.builder()
 *     .severity(AlertSeverity.ERROR)
 *     .source("eagle-system-service")
 *     .category("mq-dlq")
 *     .title("AccountRegistered 死信投递")
 *     .context("eventId", event.getEventId())
 *     .context("totalAttempts", String.valueOf(attempts))
 *     .cause(throwable)
 *     .build());
 * }</pre>
 *
 * @author sunshixiong
 */
public interface AlertService {

    /**
     * 发送一条告警事件。实现必须线程安全且不抛异常 —— 告警链路失败不能影响主业务。
     */
    void send(AlertEvent event);
}
