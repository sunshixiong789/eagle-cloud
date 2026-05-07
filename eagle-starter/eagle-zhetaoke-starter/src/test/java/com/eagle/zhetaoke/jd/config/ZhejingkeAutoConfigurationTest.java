package com.eagle.zhetaoke.jd;

import com.eagle.http.client.config.EagleHttpClientAutoConfiguration;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.zhetaoke.jd.client.ZhejingkeClient;
import com.eagle.zhetaoke.jd.config.ZhejingkeAutoConfiguration;
import com.eagle.zhetaoke.jd.properties.ZhejingkeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZhejingkeAutoConfiguration} 自动配置测试。
 *
 * @author 孙士雄
 */
class ZhejingkeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    EagleHttpClientAutoConfiguration.class,
                    ZhejingkeAutoConfiguration.class));

    @Test
    void shouldAutoConfigureZhejingkeClient() {
        contextRunner
                .withPropertyValues("eagle.zhejingke.appkey=test-key")
                .run(context -> assertThat(context)
                        .hasSingleBean(ZhejingkeProperties.class)
                        .hasSingleBean(ZhejingkeClient.class)
                        .hasSingleBean(EagleHttpServiceClientFactory.class));
    }

    @Test
    void shouldBindProperties() {
        contextRunner
                .withPropertyValues(
                        "eagle.zhejingke.enabled=true",
                        "eagle.zhejingke.appkey=my-appkey",
                        "eagle.zhejingke.base-url=https://custom.zhetaoke.com",
                        "eagle.zhejingke.connect-timeout=3s",
                        "eagle.zhejingke.read-timeout=10s")
                .run(context -> {
                    ZhejingkeProperties properties = context.getBean(ZhejingkeProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getAppkey()).isEqualTo("my-appkey");
                    assertThat(properties.getBaseUrl()).isEqualTo("https://custom.zhetaoke.com");
                });
    }

    @Test
    void shouldDisableWhenEnabledIsFalse() {
        contextRunner
                .withPropertyValues("eagle.zhejingke.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(ZhejingkeClient.class));
    }

    @Test
    void shouldHaveDefaultBaseUrl() {
        contextRunner
                .withPropertyValues("eagle.zhejingke.appkey=test")
                .run(context -> {
                    ZhejingkeProperties properties = context.getBean(ZhejingkeProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo("http://api.zhetaoke.com:20000");
                });
    }

    @Test
    void shouldRegisterPropertiesBean() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(ZhejingkeProperties.class));
    }
}
