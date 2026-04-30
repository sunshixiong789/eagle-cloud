package com.eagle.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.storage")
public class StorageProperties {

    /**
     * 存储类型：minio / local / oss。
     */
    private String type = "local";

    /**
     * MinIO 配置。
     */
    private Minio minio = new Minio();

    /**
     * 本地存储配置。
     */
    private Local local = new Local();

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
    }

    @Data
    public static class Local {
        /**
         * 本地存储根目录。
         */
        private String basePath = "/data/eagle/storage";

        /**
         * 访问 URL 前缀。
         */
        private String urlPrefix = "http://localhost:8080/storage";
    }
}
