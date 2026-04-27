package com.eagle.oss.config;

import com.eagle.oss.properties.StorageProperties;
import com.eagle.oss.service.LocalStorageServiceImpl;
import com.eagle.oss.service.MinioStorageServiceImpl;
import com.eagle.oss.service.StorageService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 文件存储自动配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.storage.type", havingValue = "minio")
    public MinioClient minioClient(StorageProperties properties) {
        log.info("MinIO storage enabled, endpoint: {}", properties.getMinio().getEndpoint());
        return MinioClient.builder()
                .endpoint(properties.getMinio().getEndpoint())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.storage.type", havingValue = "minio")
    public StorageService minioStorageService(MinioClient minioClient, StorageProperties properties) {
        return new MinioStorageServiceImpl(minioClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties properties) {
        log.info("Local storage enabled, basePath: {}", properties.getLocal().getBasePath());
        return new LocalStorageServiceImpl(properties);
    }
}
