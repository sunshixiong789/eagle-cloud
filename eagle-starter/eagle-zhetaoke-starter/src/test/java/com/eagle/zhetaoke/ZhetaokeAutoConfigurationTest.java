package com.eagle.zhetaoke;

import com.eagle.http.client.config.EagleHttpClientAutoConfiguration;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.zhetaoke.client.ZhetaokeClient;
import com.eagle.zhetaoke.config.ZhetaokeAutoConfiguration;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZhetaokeAutoConfiguration} 自动配置测试。
 *
 * @author 孙士雄
 */
class ZhetaokeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    EagleHttpClientAutoConfiguration.class,
                    ZhetaokeAutoConfiguration.class));

    @Test
    void shouldAutoConfigureZhetaokeClient() {
        contextRunner
                .withPropertyValues(
                        "eagle.zhetaoke.appkey=test-key",
                        "eagle.zhetaoke.sid=test-sid",
                        "eagle.zhetaoke.pid=mm_1_2_3")
                .run(context -> assertThat(context)
                        .hasSingleBean(ZhetaokeProperties.class)
                        .hasSingleBean(ZhetaokeClient.class)
                        .hasSingleBean(EagleHttpServiceClientFactory.class));
    }

    @Test
    void shouldBindProperties() {
        contextRunner
                .withPropertyValues(
                        "eagle.zhetaoke.enabled=true",
                        "eagle.zhetaoke.appkey=my-appkey",
                        "eagle.zhetaoke.sid=my-sid",
                        "eagle.zhetaoke.pid=mm_123_456_789",
                        "eagle.zhetaoke.base-url=https://custom.zhetaoke.com",
                        "eagle.zhetaoke.backup-url=https://backup.zhetaoke.com",
                        "eagle.zhetaoke.connect-timeout=3s",
                        "eagle.zhetaoke.read-timeout=10s")
                .run(context -> {
                    ZhetaokeProperties properties = context.getBean(ZhetaokeProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getAppkey()).isEqualTo("my-appkey");
                    assertThat(properties.getSid()).isEqualTo("my-sid");
                    assertThat(properties.getPid()).isEqualTo("mm_123_456_789");
                    assertThat(properties.getBaseUrl()).isEqualTo("https://custom.zhetaoke.com");
                    assertThat(properties.getBackupUrl()).isEqualTo("https://backup.zhetaoke.com");
                });
    }

    @Test
    void shouldDisableWhenEnabledIsFalse() {
        contextRunner
                .withPropertyValues("eagle.zhetaoke.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(ZhetaokeClient.class));
    }

    @Test
    void shouldHaveDefaultBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "eagle.zhetaoke.appkey=test",
                        "eagle.zhetaoke.sid=test",
                        "eagle.zhetaoke.pid=test")
                .run(context -> {
                    ZhetaokeProperties properties = context.getBean(ZhetaokeProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo("https://api.zhetaoke.com:10001");
                    assertThat(properties.getBackupUrl()).isEqualTo("http://api.zhetaoke.cn:10000");
                });
    }

    @Test
    void shouldRegisterPropertiesBean() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(ZhetaokeProperties.class));
    }
}
