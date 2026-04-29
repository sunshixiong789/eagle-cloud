package com.eagle.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;

/**
 * Feign 请求拦截器：透传 Seata 分布式事务 XID。
 *
 * <p>此拦截器仅在 {@code seata-spring-boot-starter} 存在于类路径时由自动配置注册，
 * 因此可安全地直接调用 {@link RootContext#getXID()} 而无需反射。
 *
 * @author 孙士雄
 */
@Slf4j
public class SeataXidRequestInterceptor implements RequestInterceptor {

    private static final String XID_HEADER = "TX_XID";

    @Override
    public void apply(RequestTemplate template) {
        String xid = RootContext.getXID();
        if (xid != null && !xid.isBlank()) {
            template.header(XID_HEADER, xid);
            log.debug("Seata XID propagated: {}", xid);
        }
    }
}
