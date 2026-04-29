package com.eagle.tenant.aspect;

import com.eagle.datasource.routing.DataSourceContextHolder;
import com.eagle.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * DATABASE 模式租户数据源路由切面。
 *
 * <p>拦截被 {@link org.springframework.transaction.annotation.Transactional} 注解标记的方法或类，
 * 将当前租户 ID 写入数据源上下文，由 {@link com.eagle.datasource.routing.DynamicDataSource}
 * 路由到对应租户的数据库。
 *
 * <p>使用此模式时，需预先在 {@code DynamicDataSource} 中注册租户 ID 与数据源的映射关系。
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
public class TenantDatabaseRoutingAspect {

    /**
     * 同时支持方法级和类级 {@code @Transactional} 注解，确保类上声明事务的方法也能被路由。
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)"
            + " || @within(org.springframework.transaction.annotation.Transactional)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return point.proceed();
        }

        DataSourceContextHolder.set(tenantId);
        log.debug("Tenant data source routed: tenantId={}", tenantId);
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clear();
            log.debug("Tenant data source cleared");
        }
    }
}
