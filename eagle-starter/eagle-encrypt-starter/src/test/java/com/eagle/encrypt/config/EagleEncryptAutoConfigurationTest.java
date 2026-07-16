package com.eagle.encrypt.config;

import com.eagle.encrypt.converter.EncryptedStringConverter;
import com.eagle.encrypt.service.EncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleEncryptAutoConfiguration} 装配条件测试。
 *
 * @author eagle
 */
class EagleEncryptAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EagleEncryptAutoConfiguration.class));

    @Test
    @DisplayName("未配置密钥时不注册 EncryptionService，转换器以透明模式装配")
    void noSecretKeyDegradesToTransparentMode() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(EncryptionService.class);
            assertThat(context).hasSingleBean(EncryptedStringConverter.class);
        });
    }

    @Test
    @DisplayName("密钥为空字符串时等同未配置，容器正常启动（compose 以 VAR=${VAR:-} 注入空串的场景）")
    void blankSecretKeyDegradesToTransparentMode() {
        runner.withPropertyValues("eagle.encrypt.secret-key=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(EncryptionService.class);
            assertThat(context).hasSingleBean(EncryptedStringConverter.class);
        });
    }

    @Test
    @DisplayName("配置密钥时注册 EncryptionService 且加解密可逆")
    void secretKeyEnablesEncryption() {
        runner.withPropertyValues("eagle.encrypt.secret-key=unit-test-secret").run(context -> {
            assertThat(context).hasSingleBean(EncryptionService.class);
            EncryptionService service = context.getBean(EncryptionService.class);
            assertThat(service.decrypt(service.encrypt("13800138000"))).isEqualTo("13800138000");
        });
    }
}
