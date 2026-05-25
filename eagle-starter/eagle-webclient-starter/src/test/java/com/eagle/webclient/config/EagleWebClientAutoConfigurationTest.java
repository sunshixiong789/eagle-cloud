package com.eagle.webclient.config;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.webclient.error.EagleWebClientErrorFilter;
import com.eagle.webclient.interceptor.PropagatingHeadersExchangeFilterFunction;
import com.eagle.webclient.support.EagleReactiveServiceClientFactory;
import com.eagle.webclient.support.EagleWebClientCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleWebClientAutoConfiguration} 自动配置测试。
 *
 * @author eagle
 */
class EagleWebClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EagleWebClientAutoConfiguration.class));

    @Test
    void shouldRegisterReactiveInfrastructureBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(HttpClientProperties.class)
                .hasSingleBean(EagleWebClientErrorFilter.class)
                .hasSingleBean(EagleWebClientCustomizer.class)
                .hasSingleBean(PropagatingHeadersExchangeFilterFunction.class)
                .hasSingleBean(EagleReactiveServiceClientFactory.class)
                .hasBean("eagleWebClientBuilder")
                .hasBean("loadBalancedWebClientBuilder"));
    }

    @Test
    void shouldShareHttpClientPropertiesPrefix() {
        contextRunner
                .withPropertyValues(
                        "eagle.http-client.connect-timeout=3s",
                        "eagle.http-client.read-timeout=8s",
                        "eagle.http-client.propagated-headers[0]=Authorization",
                        "eagle.http-client.propagated-headers[1]=X-Tenant-Id",
                        "eagle.http-client.error-handler-enabled=false")
                .run(context -> {
                    HttpClientProperties properties = context.getBean(HttpClientProperties.class);
                    assertThat(properties.getConnectTimeout()).hasSeconds(3);
                    assertThat(properties.getReadTimeout()).hasSeconds(8);
                    assertThat(properties.getPropagatedHeaders())
                            .containsExactly("Authorization", "X-Tenant-Id");
                    assertThat(properties.isErrorHandlerEnabled()).isFalse();
                });
    }
}
