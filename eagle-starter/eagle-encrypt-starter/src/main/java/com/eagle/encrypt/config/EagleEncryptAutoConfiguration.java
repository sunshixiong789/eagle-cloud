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
 * <p>引入 starter 即自动装配 {@link EncryptedStringConverter}，可直接在 JPA 实体字段
 * 通过 {@code @Convert} 标注，无密钥时以透明模式（不加密）运行，便于渐进式迁移。
 *
 * <p>实际加密能力（{@link EncryptionService} Bean）仅在配置了
 * {@code eagle.encrypt.secret-key} 时注册——未提供密钥时无法初始化 AES 服务，
 * 转换器自动退化为透明模式。
 *
 * @author eagle
 */
@AutoConfiguration
@EnableConfigurationProperties(EncryptProperties.class)
public class EagleEncryptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "eagle.encrypt.secret-key")
    public EncryptionService encryptionService(EncryptProperties properties) {
        return new AesEncryptionService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EncryptedStringConverter encryptedStringConverter() {
        return new EncryptedStringConverter();
    }
}
