package com.eagle.datasource.aspect;

import com.eagle.datasource.annotation.ReadOnly;
import com.eagle.datasource.routing.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * 只读操作 AOP 切面：自动将数据源切换到从库。
 *
 * <p>拦截 {@link ReadOnly} 注解以及 {@code @Transactional(readOnly = true)} 的方法，
 * 执行前切换为 slave，执行后通过 {@code finally} 精确恢复（原始未设置时执行 {@code clear()}
 * 而非 {@code set("master")}，防止线程池 ThreadLocal 泄漏）。
 *
 * <p>此 Bean 由 {@link com.eagle.datasource.config.DynamicDataSourceConfig} 显式注册，
 * 仅在配置了 {@code eagle.datasource.master.url} 时生效，不会污染单数据源场景。
 *
 * <p><b>注意</b>：{@code @Transactional(readOnly = true)} 的类级注解不会被 {@code @annotation}
 * 切点匹配到，此类场景需在方法上显式添加 {@link ReadOnly}。
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
@Order(-1) // 必须在事务切面（Order.LOWEST_PRECEDENCE - 1 ≈ 0）之前，确保数据源先切换
public class ReadOnlyAspect {

    @Around("@annotation(readOnly)")
    public Object aroundReadOnly(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        return switchToSlaveAndProceed(point);
    }

    @Around("@annotation(transactional)")
    public Object aroundTransactional(ProceedingJoinPoint point, Transactional transactional)
            throws Throwable {
        if (transactional.readOnly()) {
            return switchToSlaveAndProceed(point);
        }
        return point.proceed();
    }

    private Object switchToSlaveAndProceed(ProceedingJoinPoint point) throws Throwable {
        String previous = DataSourceContextHolder.getRaw();
        // 已经在 slave 上则直接执行，避免重复切换（如 @ReadOnly + @Transactional(readOnly=true) 叠加）
        if (DataSourceContextHolder.SLAVE.equals(previous)) {
            return point.proceed();
        }
        DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
        if (log.isDebugEnabled()) {
            log.debug("DataSource switched to SLAVE for: {}", point.getSignature().toShortString());
        }
        try {
            return point.proceed();
        } finally {
            // previous == null 说明线程原本没有显式设置，需 remove() 而非 set("master")
            // 否则 ThreadLocal 在线程池中永不清除，导致泄漏
            if (previous == null) {
                DataSourceContextHolder.clear();
            } else {
                DataSourceContextHolder.set(previous);
            }
        }
    }
}
