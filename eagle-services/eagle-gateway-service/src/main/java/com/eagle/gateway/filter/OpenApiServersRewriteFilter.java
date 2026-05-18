package com.eagle.gateway.filter;

import com.eagle.gateway.config.GatewayOpenApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;

/**
 * 网关 OpenAPI 文档响应改写：把下游 spec 的 {@code servers[0].url} 重写为「网关外部 URL + alias」。
 *
 * <p>下游 SpringDoc 默认从请求推断 {@code servers}，得到的是下游容器内网地址
 * （如 {@code http://172.27.0.155:8882}），Swagger UI "Try it out" 会绕过网关直连下游，
 * 外网根本访问不到。本过滤器只对 {@code GET /v3/api-docs/{alias}} 生效，把 {@code servers}
 * 改成 {@code {gateway-external-base}/{alias}}，路径前缀由 SCG {@code discovery.locator}
 * 自动生成的 {@code /<serviceId>/**} 承担（当前 alias 与 serviceId 小写一致）。
 *
 * <p>外部 base 解析顺序：{@code X-Forwarded-Proto + X-Forwarded-Host} → 请求 URI 的 scheme + authority。
 * 网关基线已开 {@code x-forwarded.*-enabled=true}，反代会得到正确的外部地址。
 *
 * <p>{@code order = -1}：先于 {@code NettyWriteResponseFilter} 包装响应，确保 decorator 生效。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eagle.gateway.openapi.discovery-enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiServersRewriteFilter implements GlobalFilter, Ordered {

    private static final String SWAGGER_CONFIG_SEGMENT = "swagger-config";
    private static final String SERVERS_FIELD = "servers";
    private static final String URL_FIELD = "url";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String DESCRIPTION_VALUE = "Via gateway";

    private final ObjectMapper objectMapper;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String alias = extractAlias(request.getPath().value());
        if (alias == null) {
            return chain.filter(exchange);
        }

        String externalBase = resolveExternalBase(request);
        String serverUrl = externalBase + "/" + alias;

        ServerHttpResponse original = exchange.getResponse();
        ServerHttpResponseDecorator decorator = new ServerHttpResponseDecorator(original) {
            @Override
            public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                MediaType contentType = getHeaders().getContentType();
                if (contentType == null || !contentType.includes(MediaType.APPLICATION_JSON)) {
                    return super.writeWith(body);
                }
                Flux<? extends DataBuffer> flux = Flux.from(body);
                return super.writeWith(DataBufferUtils.join(flux).map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    byte[] rewritten = rewriteServers(bytes, serverUrl);
                    // decorator 处理的是原始 JSON，必须按重写后长度更新 Content-Length；
                    // 同时移除 Content-Encoding 防止下游 gzip 头被透传后浏览器按 gzip 解码失败
                    HttpHeaders headers = getHeaders();
                    headers.setContentLength(rewritten.length);
                    headers.remove(HttpHeaders.CONTENT_ENCODING);
                    return bufferFactory().wrap(rewritten);
                }));
            }
        };
        return chain.filter(exchange.mutate().response(decorator).build());
    }

    /**
     * 匹配 {@code /v3/api-docs/{alias}}：alias 非空、不含 {@code /}、不是 {@code swagger-config}。
     */
    private String extractAlias(String path) {
        String prefix = GatewayOpenApiConfig.API_DOCS_PATH + "/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String alias = path.substring(prefix.length());
        if (alias.isEmpty() || alias.indexOf('/') >= 0 || SWAGGER_CONFIG_SEGMENT.equals(alias)) {
            return null;
        }
        return alias;
    }

    /**
     * 解析网关外部访问地址。
     *
     * <p>反代后链路：{@code 浏览器 → Nginx/CDN → 网关}，{@code X-Forwarded-Proto/Host}
     * 由反代注入；直连场景退化为请求 URI 自身的 scheme + authority。
     */
    private String resolveExternalBase(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String proto = firstValue(headers.getFirst("X-Forwarded-Proto"));
        String host = firstValue(headers.getFirst("X-Forwarded-Host"));
        if (proto != null && host != null) {
            return proto + "://" + host;
        }
        URI uri = request.getURI();
        String scheme = uri.getScheme();
        String authority = uri.getRawAuthority();
        if (scheme != null && authority != null) {
            return scheme + "://" + authority;
        }
        return "";
    }

    private String firstValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        int comma = header.indexOf(',');
        String first = comma > 0 ? header.substring(0, comma) : header;
        String trimmed = first.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private byte[] rewriteServers(byte[] body, String serverUrl) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!(root instanceof ObjectNode obj)) {
                return body;
            }
            obj.putArray(SERVERS_FIELD)
                    .addObject()
                    .put(URL_FIELD, serverUrl)
                    .put(DESCRIPTION_FIELD, DESCRIPTION_VALUE);
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception ex) {
            log.warn("Failed to rewrite OpenAPI servers field for url={}, keep original body", serverUrl, ex);
            return body;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
