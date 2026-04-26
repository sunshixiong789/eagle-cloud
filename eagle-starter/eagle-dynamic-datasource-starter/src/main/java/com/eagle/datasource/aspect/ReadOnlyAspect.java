package com.eagle.datasource.aspect;

import com.eagle.datasource.annotation.ReadOnly;
import com.eagle.datasource.routing.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 只读操作 AOP 切面：自动切换数据源到从库。
 *
 * <p>拦截 {@code @ReadOnly} 注解和 {@code @Transactional(readOnly = true)} 的方法，
 * 在执行前将数据源切换为 slave，执行后恢复。
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
@Component
@Order(-1) // 确保在事务切面之前执行
public class ReadOnlyAspect {

    @Around("@annotation(readOnly)")
    public Object aroundReadOnly(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        return switchToSlaveAndProceed(point);
    }

    @Around("@annotation(transactional)")
    public Object aroundTransactional(ProceedingJoinPoint point, Transactional transactional) throws Throwable {
        if (transactional.readOnly()) {
            return switchToSlaveAndProceed(point);
        }
        return point.proceed();
    }

    private Object switchToSlaveAndProceed(ProceedingJoinPoint point) throws Throwable {
        String previous = DataSourceContextHolder.get();
        DataSourceContextHolder.set(DataSourceContextHolder.SLAVE);
        if (log.isDebugEnabled()) {
            log.debug("DataSource switched to SLAVE for method: {}",
                    point.getSignature().toShortString());
        }
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.set(previous);
        }
    }
}
