package com.eagle.http.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestClient 请求拦截器：透传 Seata 分布式事务 XID。
 *
 * @author 孙士雄
 */
@Slf4j
public class SeataXidClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final String XID_HEADER = "TX_XID";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String xid = RootContext.getXID();
        if (xid != null && !xid.isBlank()) {
            request.getHeaders().set(XID_HEADER, xid);
            log.debug("Seata XID propagated: {}", xid);
        }
        return execution.execute(request, body);
    }
}
