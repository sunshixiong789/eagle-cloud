package com.eagle.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Seata XID 网关透传过滤器。
 *
 * <p>从上游请求头中读取 {@code TX_XID} 并透传给下游服务，保证分布式事务上下文在网关层不丢失。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
public class SeataXidFilter implements GlobalFilter, Ordered {

    private static final String XID_HEADER = "TX_XID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String xid = exchange.getRequest().getHeaders().getFirst(XID_HEADER);
        if (xid != null && !xid.isEmpty()) {
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(XID_HEADER, xid)
                    .build();
            log.debug("Seata XID relayed through gateway: {}", xid);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 必须在 JwtAuthenticationGlobalFilter（HIGHEST_PRECEDENCE + 100）之前执行，确保 XID 先就位
        return Ordered.HIGHEST_PRECEDENCE + 99;
    }
}
