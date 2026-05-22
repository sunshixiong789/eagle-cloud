package com.eagle.http.client.reactive.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 拦截器：透传 Seata 分布式事务 XID。
 *
 * @author 孙士雄
 */
@Slf4j
public class SeataXidExchangeFilterFunction implements ExchangeFilterFunction {

    private static final String XID_HEADER = "TX_XID";

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String xid = RootContext.getXID();
        if (xid == null || xid.isBlank()) {
            return next.exchange(request);
        }
        ClientRequest mutated = ClientRequest.from(request).header(XID_HEADER, xid).build();
        if (log.isDebugEnabled()) {
            log.debug("Seata XID propagated (reactive): {}", xid);
        }
        return next.exchange(mutated);
    }
}
