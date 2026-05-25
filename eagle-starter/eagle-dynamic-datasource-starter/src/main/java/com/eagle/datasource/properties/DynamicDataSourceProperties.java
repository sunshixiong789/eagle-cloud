package com.eagle.datasource.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 动态多数据源配置属性。
 *
 * <p>单从库使用 {@code eagle.datasource.slave.*}；多从库使用 {@code eagle.datasource.slaves[n].*}，
 * 两者同时存在时 {@code slaves} 列表优先。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.datasource")
public class DynamicDataSourceProperties {

    /**
     * 主库（写库）配置。
     */
    private SingleDataSource master = new SingleDataSource();

    /**
     * 单从库配置（向后兼容）；若同时配置了 {@link #slaves}，则此字段被忽略。
     */
    private SingleDataSource slave = new SingleDataSource();

    /**
     * 多从库配置列表；非空时取代单从库配置，路由策略为轮询。
     */
    private List<SingleDataSource> slaves = new ArrayList<>();

    /**
     * 返回实际生效的从库列表：优先使用 {@link #slaves}，兜底使用单 {@link #slave}。
     *
     * @return 不可变从库配置列表，不含 {@code null}
     */
    public List<SingleDataSource> resolveSlaves() {
        if (!slaves.isEmpty()) {
            return Collections.unmodifiableList(slaves);
        }
        if (slave.getUrl() != null && !slave.getUrl().isBlank()) {
            return List.of(slave);
        }
        return List.of();
    }

    @Data
    public static class SingleDataSource {

        /**
         * JDBC URL。
         */
        private String url;

        /**
         * 用户名。
         */
        private String username;

        /**
         * 密码（生产必须使用 Jasypt ENC() 加密）。
         */
        private String password;

        /**
         * 驱动类名（可选，Spring Boot 自动推断）。
         */
        private String driverClassName;
    }
}
