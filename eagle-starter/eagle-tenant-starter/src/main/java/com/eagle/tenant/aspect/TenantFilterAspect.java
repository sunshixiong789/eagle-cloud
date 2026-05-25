package com.eagle.tenant.aspect;

import com.eagle.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 * COLUMN 模式租户过滤器切面。
 *
 * <p>拦截被 {@link com.eagle.tenant.annotation.TenantFilter} 注解标记的方法或类，
 * 在 Hibernate Session 中启用 {@code tenantFilter} 过滤器。
 *
 * <p>若实体未定义 {@code @FilterDef(name = "tenantFilter")}，Hibernate 将抛出异常并快速失败，
 * 避免在无租户隔离的情况下静默执行查询。
 *
 * @author eagle
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class TenantFilterAspect {

    private final EntityManager entityManager;

    /**
     * 同时支持方法级和类级 {@code @TenantFilter} 注解。
     */
    @Around("@annotation(com.eagle.tenant.annotation.TenantFilter)"
            + " || @within(com.eagle.tenant.annotation.TenantFilter)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return point.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        // 若实体未定义 @FilterDef，Hibernate 会抛出 HibernateException，快速暴露配置错误
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);
        log.debug("Tenant filter enabled: tenantId={}", tenantId);
        try {
            return point.proceed();
        } finally {
            session.disableFilter("tenantFilter");
            log.debug("Tenant filter disabled");
        }
    }
}
