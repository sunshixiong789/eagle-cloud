package com.eagle.webclient.support;

import com.eagle.common.http.HttpClientProperties;
import com.eagle.webclient.error.EagleWebClientErrorFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Eagle 全局 {@link WebClient.Builder} 定制器（reactive 客户端）。
 *
 * <p>装配顺序：base（入站 header / 压测标记）→ tenant → seata → 用户业务 filter → 统一错误处理。
 *
 * @author 孙士雄
 */
@RequiredArgsConstructor
public class EagleWebClientCustomizer {

    private final HttpClientProperties properties;

    private final List<? extends ExchangeFilterFunction> baseFilters;

    private final List<? extends ExchangeFilterFunction> tenantFilters;

    private final List<? extends ExchangeFilterFunction> seataFilters;

    private final EagleWebClientErrorFilter errorFilter;

    public void customize(WebClient.Builder builder) {
        baseFilters.forEach(builder::filter);
        tenantFilters.forEach(builder::filter);
        seataFilters.forEach(builder::filter);

        if (properties.isErrorHandlerEnabled()) {
            builder.filter(errorFilter);
        }
    }
}
