package com.eagle.datasource.config;

import com.eagle.datasource.properties.DataSourceProperties;
import com.eagle.datasource.routing.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源自动配置。
 *
 * <p>当配置 {@code eagle.datasource.enabled=true} 时生效，注册主从数据源并包装为路由数据源。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnProperty(name = "eagle.datasource.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamicDataSourceConfig {

    private final DataSourceProperties properties;

    /**
     * 注册主从动态路由数据源（Primary，供 JPA / MyBatis 使用）。
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        validateDataSourceProperties();
        DataSource master = buildDataSource(properties.getMaster(), "master");
        DataSource slave = buildDataSource(properties.getSlave(), "slave");

        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", master);
        targetDataSources.put("slave", slave);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(master);

        log.info("Dynamic datasource initialized, master: {}, slave: {}",
                properties.getMaster().getUrl(), properties.getSlave().getUrl());
        return dynamicDataSource;
    }

    /**
     * 启动时快速失败：配置不完整时立即抛出异常，而非在运行时随机 NPE。
     */
    private void validateDataSourceProperties() {
        DataSourceProperties.SingleDataSource master = properties.getMaster();
        DataSourceProperties.SingleDataSource slave = properties.getSlave();
        if (master == null || master.getUrl() == null || master.getUrl().isBlank()) {
            throw new IllegalStateException(
                    "Dynamic datasource master URL must be configured (eagle.datasource.master.url)");
        }
        if (slave == null || slave.getUrl() == null || slave.getUrl().isBlank()) {
            throw new IllegalStateException(
                    "Dynamic datasource slave URL must be configured (eagle.datasource.slave.url)");
        }
    }

    private DataSource buildDataSource(DataSourceProperties.SingleDataSource config, String name) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(config.getUrl())
                .username(config.getUsername())
                .password(config.getPassword());
        if (config.getDriverClassName() != null && !config.getDriverClassName().isEmpty()) {
            builder.driverClassName(config.getDriverClassName());
        }
        log.debug("{} datasource configured: {}", name, config.getUrl());
        return builder.build();
    }
}
