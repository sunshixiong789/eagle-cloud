package com.eagle.http.client.config;

import com.eagle.http.client.error.EagleResponseErrorHandler;
import com.eagle.http.client.interceptor.PropagatingHeadersClientHttpRequestInterceptor;
import com.eagle.http.client.properties.HttpClientProperties;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.http.client.support.EagleRestClientCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EagleHttpClientAutoConfiguration} 自动配置测试。
 *
 * @author 孙士雄
 */
class EagleHttpClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    EagleHttpClientAutoConfiguration.class));

    @Test
    void shouldRegisterRestClientInfrastructureBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(HttpClientProperties.class)
                .hasSingleBean(PropagatingHeadersClientHttpRequestInterceptor.class)
                .hasSingleBean(EagleResponseErrorHandler.class)
                .hasSingleBean(EagleRestClientCustomizer.class)
                .hasSingleBean(HttpServiceProxyFactory.class)
                .hasSingleBean(EagleHttpServiceClientFactory.class)
                .hasBean("loadBalancedRestClientBuilder")
                .hasBean("restClientBuilder"));
    }

    @Test
    void shouldBindDurationAndHeaderProperties() {
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
