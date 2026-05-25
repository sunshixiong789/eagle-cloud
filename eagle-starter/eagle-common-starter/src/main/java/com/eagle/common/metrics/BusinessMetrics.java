package com.eagle.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 业务指标工具类。
 *
 * <p>封装 Micrometer API，为电商关键业务流程提供语义化指标记录接口。
 * 所有指标均以 {@code eagle.} 为前缀，可被 Prometheus 采集并在 Grafana 可视化。
 *
 * <p>典型指标：
 * <ul>
 *   <li>{@code eagle.order.created.total} — 订单创建总量（按 channel 标签分）</li>
 *   <li>{@code eagle.payment.success.total} — 支付成功总量</li>
 *   <li>{@code eagle.payment.failed.total} — 支付失败总量（按 reason 标签分）</li>
 *   <li>{@code eagle.inventory.deduct.duration} — 库存扣减耗时（P99 监控）</li>
 *   <li>{@code eagle.api.call.total} — 通用接口调用量（按 service、method 标签分）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 注入后使用
 * @RequiredArgsConstructor
 * public class OrderApplicationService {
 *     private final BusinessMetrics metrics;
 *
 *     public void createOrder(CreateOrderRequest req) {
 *         metrics.incrementOrderCreated(req.getChannel());
 *         Timer.Sample sample = metrics.startTimer();
 *         try {
 *             // ... 业务逻辑 ...
 *             metrics.recordDuration("inventory.deduct", sample);
 *         } catch (Exception e) {
 *             metrics.incrementPaymentFailed("inventory_error");
 *             throw e;
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class BusinessMetrics {

    private static final String PREFIX = "eagle.";

    private final MeterRegistry registry;

    // ──────────────────────────────────────────────────────────────────────────
    // 订单指标
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 记录订单创建成功。
     *
     * @param channel 下单渠道，如 "app"、"web"、"mini_program"
     */
    public void incrementOrderCreated(String channel) {
        counter("order.created", "channel", channel).increment();
    }

    /**
     * 记录订单取消。
     *
     * @param reason 取消原因，如 "user_cancel"、"timeout"、"stock_shortage"
     */
    public void incrementOrderCancelled(String reason) {
        counter("order.cancelled", "reason", reason).increment();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 支付指标
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 记录支付成功。
     *
     * @param paymentMethod 支付方式，如 "alipay"、"wechat"、"balance"
     */
    public void incrementPaymentSuccess(String paymentMethod) {
        counter("payment.success", "method", paymentMethod).increment();
    }

    /**
     * 记录支付失败。
     *
     * @param reason 失败原因，如 "balance_insufficient"、"timeout"、"gateway_error"
     */
    public void incrementPaymentFailed(String reason) {
        counter("payment.failed", "reason", reason).increment();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 库存指标
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 记录库存扣减成功。
     *
     * @param warehouseId 仓库 ID
     */
    public void incrementInventoryDeducted(String warehouseId) {
        counter("inventory.deducted", "warehouse", warehouseId).increment();
    }

    /**
     * 记录库存不足（扣减失败）。
     */
    public void incrementInventoryInsufficient() {
        counter("inventory.insufficient").increment();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 限流/熔断指标
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 记录被限流次数。
     *
     * @param resource 被限流的资源（接口或服务名）
     */
    public void incrementRateLimited(String resource) {
        counter("rate.limited", "resource", resource).increment();
    }

    /**
     * 记录熔断次数。
     *
     * @param service 发生熔断的下游服务
     */
    public void incrementCircuitBreaker(String service) {
        counter("circuit.breaker", "service", service).increment();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 通用耗时
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 开始计时（配合 {@link #recordDuration} 使用）。
     *
     * @return 计时器样本
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * 记录操作耗时。
     *
     * @param operation 操作名，如 "inventory.deduct"、"order.create"
     * @param sample    由 {@link #startTimer()} 返回的计时器样本
     */
    public void recordDuration(String operation, Timer.Sample sample) {
        sample.stop(timer(operation));
    }

    /**
     * 直接记录耗时（单位毫秒）。
     *
     * @param operation  操作名
     * @param durationMs 耗时（毫秒）
     */
    public void recordDuration(String operation, long durationMs) {
        timer(operation).record(Duration.ofMillis(durationMs));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 内部工具方法
    // ──────────────────────────────────────────────────────────────────────────

    private Counter counter(String name, String... tags) {
        return Counter.builder(PREFIX + name)
                .tags(tags)
                .register(registry);
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(PREFIX + name)
                .tags(tags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
