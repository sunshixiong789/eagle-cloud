package com.eagle.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.eagle.es.properties.ElasticSearchProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Elasticsearch 自动配置类。
 *
 * <p>当类路径存在 {@link ElasticsearchClient} 且配置属性 {@code eagle.elasticsearch.enabled}
 * 为 {@code true}（默认启用）时，激活此配置。
 *
 * <p>Spring Data Elasticsearch 自身会根据 {@code spring.elasticsearch.*} 完成
 * {@link ElasticsearchClient} 的创建。此配置类主要职责：
 * <ol>
 *   <li>激活 {@link ElasticSearchProperties} 属性绑定</li>
 *   <li>通过 {@link ElasticSearchEnvironmentPostProcessor} 将 eagle 属性桥接到 spring 原生属性</li>
 *   <li>输出初始化日志，便于排查配置问题</li>
 * </ol>
 *
 * @author eagle
 * @see ElasticSearchProperties
 * @see ElasticSearchEnvironmentPostProcessor
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ElasticsearchClient.class)
@EnableConfigurationProperties(ElasticSearchProperties.class)
public class ElasticSearchAutoConfiguration {

    /**
     * 配置加载完成后输出初始化日志。
     */
    @PostConstruct
    public void init() {
        log.info("[Eagle ES] Elasticsearch auto-configuration loaded.");
    }
}
