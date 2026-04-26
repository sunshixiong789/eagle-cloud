package com.eagle.datasource.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多数据源配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.datasource")
public class DataSourceProperties {

    /**
     * 是否启用动态数据源。
     */
    private boolean enabled = false;

    /**
     * 主库（写库）配置。
     */
    private SingleDataSource master = new SingleDataSource();

    /**
     * 从库（读库）配置。
     */
    private SingleDataSource slave = new SingleDataSource();

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
         * 密码。
         */
        private String password;

        /**
         * 驱动类名（可选，Spring Boot 自动推断）。
         */
        private String driverClassName;
    }
}
