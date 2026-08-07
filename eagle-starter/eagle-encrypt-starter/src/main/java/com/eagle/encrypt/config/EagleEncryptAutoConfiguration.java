package com.eagle.encrypt.config;

import com.eagle.encrypt.converter.EncryptedStringConverter;
import com.eagle.encrypt.properties.EncryptProperties;
import com.eagle.encrypt.service.AesEncryptionService;
import com.eagle.encrypt.service.EncryptionService;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 字段级加密自动配置。
 *
 * <p>引入 starter 即自动装配 {@link EncryptedStringConverter}，可直接在 JPA 实体字段
 * 通过 {@code @Convert} 标注，无密钥时以透明模式（不加密）运行，便于渐进式迁移。
 *
 * <p>实际加密能力（{@link EncryptionService} Bean）仅在配置了非空的
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
    @Conditional(OnNonBlankSecretKeyCondition.class)
    public EncryptionService encryptionService(EncryptProperties properties) {
        return new AesEncryptionService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EncryptedStringConverter encryptedStringConverter() {
        return new EncryptedStringConverter();
    }

    /**
     * 密钥非空白时才匹配。容器编排常以 {@code VAR=${VAR:-}} 注入空串环境变量，
     * 空串必须与未配置等同（{@code @ConditionalOnProperty} 会把空串视为已配置）。
     */
    static class OnNonBlankSecretKeyCondition extends SpringBootCondition {

        @Override
        public @NonNull ConditionOutcome getMatchOutcome(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            String secretKey = context.getEnvironment().getProperty("eagle.encrypt.secret-key");
            if (StringUtils.hasText(secretKey)) {
                return ConditionOutcome.match("eagle.encrypt.secret-key 已配置");
            }
            return ConditionOutcome.noMatch("eagle.encrypt.secret-key 未配置或为空，加密服务不启用");
        }
    }
}
