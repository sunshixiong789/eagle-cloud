package com.eagle.datasource.routing;

/**
 * 动态数据源上下文持有者。
 *
 * <p>使用 ThreadLocal 存储当前线程的数据源 key，保证线程安全。
 *
 * @author 孙士雄
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static final String MASTER = "master";
    public static final String SLAVE = "slave";

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
     * 获取当前数据源，默认为 master。
     *
     * @return 数据源 key
     */
    public static String get() {
        String key = CONTEXT.get();
        return key != null ? key : MASTER;
    }

    /**
     * 清除当前数据源设置。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
