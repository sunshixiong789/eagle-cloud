package com.eagle.http.client.reactive;

import com.eagle.http.client.config.EagleHttpClientAutoConfiguration;
import com.eagle.http.client.reactive.filter.PropagatingHeadersExchangeFilterFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@code WebClient} 相关装配仅在 {@code spring-webflux} 存在时启用。
 *
 * @author 孙士雄
 */
class EagleWebClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    EagleHttpClientAutoConfiguration.class));

    @Test
    void shouldRegisterReactiveBeansWhenWebFluxOnClasspath() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(PropagatingHeadersExchangeFilterFunction.class)
                .hasSingleBean(EagleWebClientCustomizer.class)
                .hasBean("eagleWebClientBuilder"));
    }

    @Test
    void eagleWebClientBuilderProducesUsableClient() {
        contextRunner.run(context -> {
            WebClient.Builder builder = context.getBean("eagleWebClientBuilder", WebClient.Builder.class);
            WebClient client = builder.baseUrl("http://localhost").build();
            assertThat(client).isNotNull();
        });
    }
}
