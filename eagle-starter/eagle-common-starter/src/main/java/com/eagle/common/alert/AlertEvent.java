package com.eagle.common.alert;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * 告警事件载荷。
 *
 * <p>由业务代码构造,经 {@link AlertService#send(AlertEvent)} 发出。
 * 默认实现 {@link LoggingAlertService} 把字段写入 MDC + 结构化 ERROR 日志,
 * 运维侧 Logback {@code WebhookAppender} 根据 {@code alert.category} 与 {@code alert.severity}
 * 过滤后转钉钉/企微/邮件。
 *
 * @param severity 严重程度,见 {@link AlertSeverity}
 * @param source   告警源(服务名 / 模块名),通常用 {@code spring.application.name}
 * @param category 业务分类,例如 {@code mq-dlq}、{@code rpc-circuit-open}、{@code idempotency-violation}
 * @param title    简短标题({@code < 80} 字符),用于 webhook 卡片主体
 * @param message  详细描述,可换行
 * @param cause    异常(可选)
 * @author sunshixiong
 */
public record AlertEvent(
        AlertSeverity severity,
        String source,
        String category,
        String title,
        String message,
        Map<String, String> contexts,
        @Nullable Throwable cause,
        Instant occurredAt
) {

    /**
     * 在 builder 未显式赋值时填充默认值,保证 {@code occurredAt} 永不为 null。
     */
    public AlertEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (severity == null) {
            severity = AlertSeverity.ERROR;
        }
    }
}
