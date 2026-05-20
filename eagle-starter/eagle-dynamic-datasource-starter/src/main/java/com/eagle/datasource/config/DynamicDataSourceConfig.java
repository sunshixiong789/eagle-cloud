package com.eagle.datasource.config;

import com.eagle.datasource.aspect.ReadOnlyAspect;
import com.eagle.datasource.properties.DynamicDataSourceProperties;
import com.eagle.datasource.routing.DataSourceContextHolder;
import com.eagle.datasource.routing.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态数据源自动配置。
 *
 * <p>当 {@code eagle.datasource.enabled=true} 时生效，注册主从路由数据源、只读切面、
 * 以及 Async 上下文传播装饰器。
 *
 * @author 孙士雄
 */
@Slf4j
// beforeName 避免编译期依赖 spring-boot-autoconfigure 的具体模块
@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@ConditionalOnClass(AbstractRoutingDataSource.class)
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
@ConditionalOnProperty(name = "eagle.datasource.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamicDataSourceConfig {

    private final DynamicDataSourceProperties properties;

    /**
     * 脱敏 JDBC URL 中内嵌的密码（如 {@code jdbc:mysql://user:pass@host/db}）。
     */
    private static String maskUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(://[^:/@]+:)[^@]+@", "$1***@");
    }

    /**
     * 注册主从动态路由数据源（Primary，供 JPA / MyBatis 使用）。
     *
     * <p>多从库时以 {@code "slave-0"}、{@code "slave-1"} 等 key 注册，路由层轮询选择。
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        validate();
        DataSource master = buildDataSource(properties.getMaster(), "master");
        List<DynamicDataSourceProperties.SingleDataSource> slaveConfigs = properties.resolveSlaves();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceContextHolder.MASTER, master);

        if (slaveConfigs.size() == 1) {
            targetDataSources.put(DataSourceContextHolder.SLAVE,
                    buildDataSource(slaveConfigs.get(0), "slave"));
        } else {
            for (int i = 0; i < slaveConfigs.size(); i++) {
                targetDataSources.put("slave-" + i,
                        buildDataSource(slaveConfigs.get(i), "slave-" + i));
            }
        }

        DynamicDataSource dynamicDataSource = new DynamicDataSource(slaveConfigs.size());
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(master);

        log.info("Dynamic datasource initialized: master=[{}], slaves=[{}]",
                maskUrl(properties.getMaster().getUrl()),
                slaveConfigs.stream()
                        .map(s -> maskUrl(s.getUrl()))
                        .collect(Collectors.joining(", ")));
        return dynamicDataSource;
    }

    /**
     * 注册只读切面 Bean（不使用 @Component，避免依赖 ComponentScan）。
     */
    @Bean
    @ConditionalOnMissingBean
    public ReadOnlyAspect readOnlyAspect() {
        return new ReadOnlyAspect();
    }

    /**
     * 为 @Async 线程池提供数据源上下文传播装饰器。
     *
     * <p>若已有其他 {@link TaskDecorator} Bean（如 tenant 上下文传播），则跳过注册。
     * 需要组合多个装饰器时，应在应用层手动包装后注册。
     */
    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator dataSourceContextTaskDecorator() {
        return runnable -> {
            String key = DataSourceContextHolder.getRaw();
            return () -> {
                if (key != null) {
                    DataSourceContextHolder.set(key);
                }
                try {
                    runnable.run();
                } finally {
                    DataSourceContextHolder.clear();
                }
            };
        };
    }

    private void validate() {
        DynamicDataSourceProperties.SingleDataSource master = properties.getMaster();
        if (master.getUrl() == null || master.getUrl().isBlank()) {
            throw new IllegalStateException(
                    "Dynamic datasource master URL must be configured (eagle.datasource.master.url)");
        }
        if (properties.resolveSlaves().isEmpty()) {
            throw new IllegalStateException(
                    "At least one slave datasource must be configured " +
                            "(eagle.datasource.slave.url or eagle.datasource.slaves[0].url)");
        }
    }

    private DataSource buildDataSource(DynamicDataSourceProperties.SingleDataSource config,
                                       String name) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(config.getUrl())
                .username(config.getUsername())
                .password(config.getPassword());
        if (StringUtils.hasText(config.getDriverClassName())) {
            builder.driverClassName(config.getDriverClassName());
        }
        log.debug("{} datasource configured: {}", name, maskUrl(config.getUrl()));
        return builder.build();
    }
}
