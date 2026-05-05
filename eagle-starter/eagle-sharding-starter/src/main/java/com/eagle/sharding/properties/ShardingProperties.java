package com.eagle.sharding.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分库分表配置属性。
 *
 * <p>通过 ShardingSphere YAML 配置文件驱动，支持水平分库、水平分表、读写分离等能力。
 * DataSource 由 ShardingSphere 接管后对上层 JPA / MyBatis 完全透明。
 *
 * <p>示例（application.yml）：
 * <pre>
 * eagle:
 *   sharding:
 *     enabled: true
 *     config-file: classpath:sharding.yaml
 * </pre>
 *
 * <p>sharding.yaml 参考格式（水平分表）：
 * <pre>
 * databaseName: eagle_db
 * dataSources:
 *   ds0:
 *     dataSourceClassName: com.zaxxer.hikari.HikariDataSource
 *     driverClassName: com.mysql.cj.jdbc.Driver
 *     jdbcUrl: jdbc:mysql://localhost:3306/eagle_db0
 *     username: root
 *     password: root
 *   ds1:
 *     dataSourceClassName: com.zaxxer.hikari.HikariDataSource
 *     driverClassName: com.mysql.cj.jdbc.Driver
 *     jdbcUrl: jdbc:mysql://localhost:3306/eagle_db1
 *     username: root
 *     password: root
 * rules:
 *   - !SHARDING
 *     tables:
 *       t_order:
 *         actualDataNodes: ds${0..1}.t_order_${0..3}
 *         databaseStrategy:
 *           standard:
 *             shardingColumn: user_id
 *             shardingAlgorithmName: inline_db
 *         tableStrategy:
 *           standard:
 *             shardingColumn: order_id
 *             shardingAlgorithmName: inline_table
 *     shardingAlgorithms:
 *       inline_db:
 *         type: INLINE
 *         props:
 *           algorithm-expression: ds${user_id % 2}
 *       inline_table:
 *         type: INLINE
 *         props:
 *           algorithm-expression: t_order_${order_id % 4}
 * props:
 *   sql-show: false
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.sharding")
public class ShardingProperties {

    /**
     * 是否启用分库分表。
     * 默认 false，需显式配置 {@code eagle.sharding.enabled=true} 才会替换默认 DataSource。
     */
    private boolean enabled = false;

    /**
     * ShardingSphere YAML 配置文件路径。
     * 支持 {@code classpath:} 和 {@code file:} 前缀。
     * 配置格式参考 ShardingSphere 官方文档：
     * <a href="https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/yaml-config/">YAML 配置</a>
     */
    private String configFile = "classpath:sharding.yaml";
}
