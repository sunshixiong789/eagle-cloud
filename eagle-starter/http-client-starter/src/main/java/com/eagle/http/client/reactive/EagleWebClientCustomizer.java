package com.eagle.http.client.reactive;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Eagle 全局 {@link WebClient} 定制器。
 *
 * <p>把基础透传 / 租户 / Seata XID 三组 {@link ExchangeFilterFunction} 顺序追加到任意
 * 通过 {@code WebClient.Builder} 构建的客户端上，保持与 {@code RestClient} 侧同等语义。
 *
 * @author 孙士雄
 */
@RequiredArgsConstructor
public class EagleWebClientCustomizer implements WebClientCustomizer {

    private final List<? extends ExchangeFilterFunction> baseFilters;

    private final List<? extends ExchangeFilterFunction> tenantFilters;

    private final List<? extends ExchangeFilterFunction> seataFilters;

    @Override
    public void customize(WebClient.Builder builder) {
        baseFilters.forEach(builder::filter);
        tenantFilters.forEach(builder::filter);
        seataFilters.forEach(builder::filter);
    }
}
