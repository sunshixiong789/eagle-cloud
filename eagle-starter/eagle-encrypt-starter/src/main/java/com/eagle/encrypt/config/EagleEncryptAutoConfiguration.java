package com.eagle.encrypt.config;

import com.eagle.encrypt.converter.EncryptedStringConverter;
import com.eagle.encrypt.properties.EncryptProperties;
import com.eagle.encrypt.service.AesEncryptionService;
import com.eagle.encrypt.service.EncryptionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 字段级加密自动配置。
 *
 * <p>需要显式开启（{@code eagle.encrypt.enabled=true}）且提供密钥才生效。
 * 未开启时 {@link EncryptedStringConverter} 仍可声明在实体字段，以透明模式（不加密）运行，
 * 便于渐进式迁移。
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnProperty(name = "eagle.encrypt.enabled", havingValue = "true")
@EnableConfigurationProperties(EncryptProperties.class)
public class EagleEncryptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EncryptionService encryptionService(EncryptProperties properties) {
        return new AesEncryptionService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EncryptedStringConverter encryptedStringConverter() {
        return new EncryptedStringConverter();
    }
}
