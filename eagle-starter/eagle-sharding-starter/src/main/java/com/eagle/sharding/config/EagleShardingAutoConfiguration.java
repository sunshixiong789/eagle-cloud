package com.eagle.sharding.config;

import com.eagle.sharding.properties.ShardingProperties;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * 分库分表自动配置。
 *
 * <p>基于 Apache ShardingSphere JDBC 5.x 实现分库分表、读写分离能力。
 * 注册的 {@link DataSource} 标记为 {@link Primary}，对上层 JPA / MyBatis 完全透明。
 *
 * <p>生效条件：
 * <ol>
 *   <li>{@code shardingsphere-jdbc} 在类路径中</li>
 *   <li>对应 YAML 配置文件存在（默认 {@code classpath:sharding.yaml}，
 *       通过 {@code eagle.sharding.config-file} 自定义）</li>
 *   <li>未存在用户自定义 {@link DataSource} Bean（允许覆盖）</li>
 * </ol>
 *
 * <p>分片规则通过 {@code ShardingProperties.getConfigFile()} 指定的 YAML 文件声明，
 * 支持水平分库、水平分表、读写分离及其组合。
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnClass(YamlShardingSphereDataSourceFactory.class)
@ConditionalOnResource(resources = "${eagle.sharding.config-file:classpath:sharding.yaml}")
@EnableConfigurationProperties(ShardingProperties.class)
public class EagleShardingAutoConfiguration {

    /**
     * 创建 ShardingSphere 数据源。
     *
     * <p>从 {@code ShardingProperties.getConfigFile()} 读取 YAML 配置文件，
     * 通过 {@link YamlShardingSphereDataSourceFactory} 构建分片 DataSource，
     * 注册为 {@link Primary} Bean 替换 Spring Boot 默认数据源。
     *
     * @param properties     分片配置属性
     * @param resourceLoader Spring 资源加载器（支持 classpath:/file: 路径）
     * @return ShardingSphere 分片数据源
     * @throws IOException  配置文件不存在或读取失败
     * @throws SQLException ShardingSphere 初始化失败
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource shardingDataSource(ShardingProperties properties, ResourceLoader resourceLoader)
            throws IOException, SQLException {
        Resource resource = resourceLoader.getResource(properties.getConfigFile());
        try (InputStream inputStream = resource.getInputStream()) {
            return YamlShardingSphereDataSourceFactory.createDataSource(inputStream.readAllBytes());
        }
    }
}
