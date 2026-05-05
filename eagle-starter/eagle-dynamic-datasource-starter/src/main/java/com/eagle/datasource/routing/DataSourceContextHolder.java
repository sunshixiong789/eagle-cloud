package com.eagle.datasource.routing;

import org.jspecify.annotations.Nullable;

/**
 * 动态数据源上下文持有者。
 *
 * <p>使用 {@link ThreadLocal} 存储当前线程的数据源 key，保证线程安全。
 * 异步任务需通过 {@code DataSourceContextTaskDecorator} 传播上下文。
 *
 * @author 孙士雄
 */
public class DataSourceContextHolder {

    public static final String MASTER = "master";
    public static final String SLAVE = "slave";

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    /**
     * 设置当前数据源。
     *
     * @param dataSourceKey {@code "master"} 或 {@code "slave"}
     */
    public static void set(String dataSourceKey) {
        CONTEXT.set(dataSourceKey);
    }

    /**
     * 获取当前数据源，若未显式设置则默认返回 {@link #MASTER}。
     *
     * @return 数据源 key，不为 null
     */
    public static String get() {
        String key = CONTEXT.get();
        return key != null ? key : MASTER;
    }

    /**
     * 获取 ThreadLocal 中的原始值，未设置时返回 {@code null}。
     *
     * <p>用于在切面中区分"未设置"与"显式设置为 master"，以便正确清理 ThreadLocal。
     *
     * @return 原始数据源 key，或 {@code null}
     */
    @Nullable
    public static String getRaw() {
        return CONTEXT.get();
    }

    /**
     * 清除当前数据源设置，防止线程池中 ThreadLocal 泄漏。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
