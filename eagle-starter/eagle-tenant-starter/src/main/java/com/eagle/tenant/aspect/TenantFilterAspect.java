package com.eagle.tenant.aspect;

import com.eagle.tenant.TenantContextHolder;
import com.eagle.tenant.annotation.TenantFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * COLUMN 模式租户过滤器切面。
 *
 * <p>拦截 {@link TenantFilter} 注解的方法，在 Hibernate Session 中启用 {@code tenantFilter}。
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "eagle.tenant", name = "mode", havingValue = "column")
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@annotation(tenantFilter)")
    public Object around(ProceedingJoinPoint point, TenantFilter tenantFilter) throws Throwable {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return point.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter filter = null;
        try {
            filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
            log.debug("Tenant filter enabled: tenantId={}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to enable tenant filter, ensure @FilterDef(name=\"tenantFilter\") is defined on entity", e);
        }

        try {
            return point.proceed();
        } finally {
            if (filter != null) {
                session.disableFilter("tenantFilter");
                log.debug("Tenant filter disabled");
            }
        }
    }
}
