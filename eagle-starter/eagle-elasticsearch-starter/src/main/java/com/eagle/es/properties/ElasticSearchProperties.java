package com.eagle.es.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Elasticsearch 配置属性。
 *
 * <p>通过 {@code eagle.elasticsearch.*} 前缀绑定，统一管理集群连接参数。
 * 属性值将由 {@link com.eagle.es.config.ElasticSearchEnvironmentPostProcessor}
 * 桥接到 Spring Data Elasticsearch 原生属性（{@code spring.elasticsearch.*}）。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.elasticsearch")
public class ElasticSearchProperties {

    /** Elasticsearch 节点地址列表，如 http://localhost:9200 */
    private List<String> uris = List.of("http://localhost:9200");

    /** 用户名（可选） */
    private String username;

    /** 密码（可选） */
    private String password;

    /** 连接超时（ms） */
    private int connectTimeout = 5000;

    /** Socket 超时（ms） */
    private int socketTimeout = 30000;

    /** 是否启用 SSL */
    private boolean sslEnabled = false;
}
