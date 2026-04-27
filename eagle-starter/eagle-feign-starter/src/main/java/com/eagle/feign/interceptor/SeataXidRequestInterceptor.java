package com.eagle.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 请求拦截器：透传 Seata 分布式事务 XID。
 *
 * <p>通过反射获取 {@code RootContext.getXID()}，避免对 Seata 产生强依赖。
 *
 * @author 孙士雄
 */
@Slf4j
public class SeataXidRequestInterceptor implements RequestInterceptor {

    private static final String XID_HEADER = "TX_XID";

    @Override
    public void apply(RequestTemplate template) {
        String xid = getXid();
        if (xid != null && !xid.isEmpty()) {
            template.header(XID_HEADER, xid);
            log.debug("Seata XID propagated via Feign: {}", xid);
        }
    }

    private String getXid() {
        try {
            Class<?> rootContext = Class.forName("org.apache.seata.core.context.RootContext");
            return (String) rootContext.getMethod("getXID").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }
}
