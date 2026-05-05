package com.eagle.datasource.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态数据源路由实现。
 *
 * <p>单从库时直接使用 {@code "slave"} 作为路由 key；
 * 多从库时按轮询策略依次使用 {@code "slave-0"}、{@code "slave-1"} 等，
 * 对应 key 在 {@link com.eagle.datasource.config.DynamicDataSourceConfig} 中注册。
 *
 * @author 孙士雄
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private final int slaveCount;
    private final AtomicInteger slaveIndex = new AtomicInteger(0);

    /**
     * @param slaveCount 实际从库数量，1 表示单从库（不启用轮询）
     */
    public DynamicDataSource(int slaveCount) {
        this.slaveCount = slaveCount;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = DataSourceContextHolder.get();
        if (DataSourceContextHolder.SLAVE.equals(key) && slaveCount > 1) {
            // 使用位掩码保证非负，再取模轮询
            int idx = (slaveIndex.getAndIncrement() & Integer.MAX_VALUE) % slaveCount;
            return "slave-" + idx;
        }
        return key;
    }
}
