package com.eagle.websocket.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket 指标收集器。
 *
 * <p>统计在线连接数、消息发送量等关键指标，上报给 Micrometer，
 * 可通过 Prometheus + Grafana 可视化监控 WebSocket 服务健康状况。
 *
 * <p>已注册的指标：
 * <ul>
 *   <li>{@code eagle.websocket.connections.active} — 实时在线连接数（Gauge）</li>
 *   <li>{@code eagle.websocket.messages.sent} — 累计消息发送数（Counter）</li>
 * </ul>
 *
 * <p>使用方式：在 {@link com.eagle.websocket.session.WebSocketSessionManager}
 * 发送消息时调用 {@link #onMessageSent()}；在 WebSocket 会话事件监听器中
 * 调用 {@link #onConnect()} 和 {@link #onDisconnect()}。
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class WebSocketMetrics {

    /**
     * 当前活跃连接数
     */
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /**
     * 累计消息发送总数（用于内部统计，实际指标通过 Counter 上报）
     */
    private final AtomicLong totalMessagesSent = new AtomicLong(0);

    private final MeterRegistry meterRegistry;

    /**
     * 初始化 Micrometer 指标注册，在 Bean 注入完成后执行。
     *
     * <p>注册 Gauge 实时反映当前连接数，Gauge 值与 {@link #activeConnections} 绑定，
     * 每次 Prometheus 抓取时动态读取最新值。
     */
    @PostConstruct
    public void init() {
        Gauge.builder("eagle.websocket.connections.active", activeConnections, AtomicInteger::get)
                .description("Active WebSocket connections")
                .register(meterRegistry);
    }

    /**
     * 客户端建立连接时调用，活跃连接数加一。
     */
    public void onConnect() {
        activeConnections.incrementAndGet();
    }

    /**
     * 客户端断开连接时调用，活跃连接数减一（最小为 0）。
     */
    public void onDisconnect() {
        activeConnections.updateAndGet(current -> Math.max(0, current - 1));
    }

    /**
     * 成功发送一条消息时调用，累计计数并上报 Counter 指标。
     */
    public void onMessageSent() {
        totalMessagesSent.incrementAndGet();
        Counter.builder("eagle.websocket.messages.sent")
                .description("Total WebSocket messages sent")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 获取当前活跃连接数（实时快照）。
     *
     * @return 当前活跃 WebSocket 连接数
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * 获取累计消息发送总数（自服务启动以来）。
     *
     * @return 累计消息发送数
     */
    public long getTotalMessagesSent() {
        return totalMessagesSent.get();
    }
}
