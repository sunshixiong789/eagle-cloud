package com.eagle.common.alert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoggingAlertService")
class LoggingAlertServiceTest {

    private final LoggingAlertService service = new LoggingAlertService();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingAlertService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    @DisplayName("送告警事件写 ERROR 日志,字段格式化到 message + ctx KV")
    void sendsErrorWithFormattedContext() {
        AlertEvent event = new AlertEvent(
                AlertSeverity.ERROR,
                "eagle-system-service",
                "mq-dlq",
                "AccountRegistered 死信",
                "16 次重试均失败",
                Map.of("eventId", "01HQ-XYZ", "totalAttempts", "16"),
                null,
                null);

        service.send(event);

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(1);
        ILoggingEvent log = events.get(0);
        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        String rendered = log.getFormattedMessage();
        assertThat(rendered)
                .contains("[ERROR]")
                .contains("[mq-dlq]")
                .contains("AccountRegistered 死信")
                .contains("16 次重试均失败")
                .contains("eventId=01HQ-XYZ")
                .contains("totalAttempts=16");
    }

    @Test
    @DisplayName("MDC 标签在发送期间填充,发送后清理")
    void mdcIsClearedAfterSend() {
        AlertEvent event = new AlertEvent(
                AlertSeverity.WARN, "svc", "rpc-circuit-open", "title", "msg",
                null, null, null);

        service.send(event);

        // 同线程发送后 MDC 应清理干净
        assertThat(MDC.get("alert.severity")).isNull();
        assertThat(MDC.get("alert.source")).isNull();
        assertThat(MDC.get("alert.category")).isNull();
    }

    @Test
    @DisplayName("携带 cause 时日志含异常堆栈")
    void includesStackTraceWhenCausePresent() {
        AlertEvent event = new AlertEvent(
                AlertSeverity.CRITICAL, "svc", "data-corruption", "数据损坏", "foo bar",
                null, new IllegalStateException("boom"), null);

        service.send(event);

        ILoggingEvent log = appender.list.get(0);
        assertThat(log.getThrowableProxy()).isNotNull();
        assertThat(log.getThrowableProxy().getClassName())
                .isEqualTo(IllegalStateException.class.getName());
    }

    @Test
    @DisplayName("null 事件直接静默返回,不抛异常")
    void nullEventSilentlyIgnored() {
        service.send(null);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("severity 传 null,compact ctor 回退为 ERROR")
    void severityDefaultsToErrorWhenMissing() {
        AlertEvent event = new AlertEvent(
                null, "svc", "test", "t", "m", null, null, null);

        assertThat(event.severity()).isEqualTo(AlertSeverity.ERROR);
    }

    @Test
    @DisplayName("occurredAt 未显式 → compact ctor 填充非 null")
    void occurredAtDefaultsToNow() {
        AlertEvent event = new AlertEvent(
                AlertSeverity.INFO, "svc", "test", "t", "m", null, null, null);

        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("空 contexts 渲染为 {}")
    void emptyContextsRendered() {
        AlertEvent event = new AlertEvent(
                AlertSeverity.INFO, "svc", "test", "t", "m", null, null, null);

        service.send(event);

        assertThat(appender.list.get(0).getFormattedMessage()).contains("ctx={}");
    }
}
