package com.eagle.datasource.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由实现。
 *
 * <p>基于 {@link AbstractRoutingDataSource}，根据当前线程上下文决定使用主库还是从库。
 *
 * @author 孙士雄
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
}
