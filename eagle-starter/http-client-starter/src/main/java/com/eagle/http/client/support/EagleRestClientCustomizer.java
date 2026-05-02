package com.eagle.http.client.support;

import com.eagle.http.client.error.EagleResponseErrorHandler;
import com.eagle.http.client.properties.HttpClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Eagle 全局 RestClient 定制器。
 *
 * @author 孙士雄
 */
@RequiredArgsConstructor
public class EagleRestClientCustomizer implements RestClientCustomizer {

    private final HttpClientProperties properties;

    private final List<? extends ClientHttpRequestInterceptor> baseInterceptors;

    private final List<? extends ClientHttpRequestInterceptor> tenantInterceptors;

    private final List<? extends ClientHttpRequestInterceptor> seataInterceptors;

    private final EagleResponseErrorHandler errorHandler;

    @Override
    public void customize(RestClient.Builder builder) {
        builder.requestInterceptors(interceptors -> {
            interceptors.addAll(baseInterceptors);
            interceptors.addAll(tenantInterceptors);
            interceptors.addAll(seataInterceptors);
        });

        if (properties.isErrorHandlerEnabled()) {
            builder.defaultStatusHandler(errorHandler);
        }

        if (properties.isBufferContent()) {
            builder.bufferContent((uri, method) -> true);
        }

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(properties.getConnectTimeout(), properties.getReadTimeout());
        builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
    }
}
