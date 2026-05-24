package com.eagle.common.alert;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

/**
 * 默认 {@link AlertService} 实现:以结构化 ERROR 日志 + MDC 标签输出。
 *
 * <p>MDC 字段:
 * <ul>
 *   <li>{@code alert.severity} — 取自 {@link AlertEvent#severity()}</li>
 *   <li>{@code alert.source} — 取自 {@link AlertEvent#source()}</li>
 *   <li>{@code alert.category} — 取自 {@link AlertEvent#category()}</li>
 * </ul>
 *
 * <p>Logback 侧的 {@code WebhookAppender}(由运维配置)按 {@code alert.category} 与
 * {@code alert.severity} 过滤,把高优先级告警转钉钉/企微。开发期没有 webhook 配置时,
 * 日志即文件留痕,不丢信息。
 *
 * <p>实现保证不抛异常:任何异常都会被捕获并降级为 WARN 日志,
 * 避免告警链路失败影响主业务调用者。
 *
 * @author sunshixiong
 */
@Slf4j
public class LoggingAlertService implements AlertService {

    /** MDC key 前缀,与 Logback {@code <discriminator>} / {@code <filter>} 配置对齐。 */
    public static final String MDC_PREFIX = "alert.";

    @Override
    public void send(AlertEvent event) {
        if (event == null) {
            return;
        }
        String severityKey = MDC_PREFIX + "severity";
        String sourceKey = MDC_PREFIX + "source";
        String categoryKey = MDC_PREFIX + "category";
        try {
            MDC.put(severityKey, String.valueOf(event.severity()));
            if (event.source() != null) {
                MDC.put(sourceKey, event.source());
            }
            if (event.category() != null) {
                MDC.put(categoryKey, event.category());
            }
            // 上下文键值对 → 单行 KV,便于 ELK / Grafana 解析
            String contextStr = formatContext(event.contexts());
            if (event.cause() != null) {
                log.error("ALERT [{}][{}] {} | {} | ctx={}",
                        event.severity(), event.category(), event.title(),
                        event.message(), contextStr, event.cause());
            } else {
                log.error("ALERT [{}][{}] {} | {} | ctx={}",
                        event.severity(), event.category(), event.title(),
                        event.message(), contextStr);
            }
        } catch (RuntimeException ex) {
            // 告警链路自身失败不能影响业务,降级 WARN
            log.warn("send alert failed, category={}, title={}",
                    event.category(), event.title(), ex);
        } finally {
            MDC.remove(severityKey);
            MDC.remove(sourceKey);
            MDC.remove(categoryKey);
        }
    }

    private String formatContext(Map<String, String> ctx) {
        if (ctx == null || ctx.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            if (i++ > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.append('}').toString();
    }
}
