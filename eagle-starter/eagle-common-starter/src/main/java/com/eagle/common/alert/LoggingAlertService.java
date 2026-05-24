package com.eagle.common.alert;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
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
 * <p>MDC 用 {@link MDC#putCloseable} + try-with-resources 管理,保证 close 顺序与异常路径都能清理。
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
        List<MDC.MDCCloseable> handles = new ArrayList<>(3);
        try {
            handles.add(MDC.putCloseable(MDC_PREFIX + "severity", String.valueOf(event.severity())));
            if (event.source() != null) {
                handles.add(MDC.putCloseable(MDC_PREFIX + "source", event.source()));
            }
            if (event.category() != null) {
                handles.add(MDC.putCloseable(MDC_PREFIX + "category", event.category()));
            }
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
            // 反向 close 保证恢复 MDC 原值(若有嵌套)
            for (int i = handles.size() - 1; i >= 0; i--) {
                handles.get(i).close();
            }
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
