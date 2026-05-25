package com.eagle.common.pressuretest;

/**
 * 全链路压测上下文。
 *
 * <p>通过 ThreadLocal 标记当前请求是否为压测流量（影子流量）。
 * 压测流量应路由到影子库/影子 Topic，与生产数据完全隔离，防止污染线上数据。
 *
 * <p>压测标记通过 HTTP 请求头 {@code X-Eagle-Gray: true} 传入，
 * 由 {@code PressureTestFilter} 负责读取并写入 ThreadLocal，
 * 由 Feign 拦截器负责在跨服务调用时继续透传该头。
 *
 * <p>典型使用：
 * <pre>{@code
 * // 数据库路由：压测请求走影子库
 * if (PressureTestContext.isPressureTest()) {
 *     DataSourceContextHolder.setShadow();
 * }
 *
 * // RocketMQ Topic：压测请求写影子 Topic
 * String topic = PressureTestContext.isPressureTest()
 *     ? "shadow_" + normalTopic : normalTopic;
 * }</pre>
 *
 * @author eagle
 */
public final class PressureTestContext {

    /**
     * 压测流量请求头名称
     */
    public static final String PRESSURE_TEST_HEADER = "X-Eagle-Gray";

    private static final ThreadLocal<Boolean> PRESSURE_TEST_FLAG = ThreadLocal.withInitial(() -> false);

    private PressureTestContext() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 标记当前线程为压测流量。
     */
    public static void mark() {
        PRESSURE_TEST_FLAG.set(true);
    }

    /**
     * 判断当前线程是否为压测流量。
     *
     * @return {@code true} 为压测流量，{@code false} 为正常流量
     */
    public static boolean isPressureTest() {
        return Boolean.TRUE.equals(PRESSURE_TEST_FLAG.get());
    }

    /**
     * 清除压测标记。
     *
     * <p><b>必须在请求结束时调用</b>，防止 ThreadLocal 内存泄漏（线程池复用线程场景）。
     * 通常在 Filter 的 finally 块中调用。
     */
    public static void clear() {
        PRESSURE_TEST_FLAG.remove();
    }
}
