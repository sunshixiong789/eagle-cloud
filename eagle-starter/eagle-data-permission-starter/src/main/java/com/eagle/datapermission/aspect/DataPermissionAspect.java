package com.eagle.datapermission.aspect;

import com.eagle.datapermission.annotation.DataPermission;
import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.helper.DataPermissionHelper;
import com.eagle.datapermission.provider.DataPermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据权限 AOP 切面。
 *
 * <p>拦截 {@code @DataPermission} 注解的方法，自动为第一个 {@link Specification} 参数
 * 注入数据权限过滤条件。
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(-2) // 在 ReadOnlyAspect 之前执行
public class DataPermissionAspect {

    private final DataPermissionProvider provider;

    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint point, DataPermission dataPermission) throws Throwable {
        if (provider == null) {
            log.warn("DataPermissionProvider not available, skipping data permission filter");
            return point.proceed();
        }

        DataScope scope = provider.getCurrentUserDataScope();
        if (scope == DataScope.ALL) {
            return point.proceed();
        }

        Object[] args = point.getArgs();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?>[] paramTypes = signature.getParameterTypes();

        for (int i = 0; i < paramTypes.length; i++) {
            if (Specification.class.isAssignableFrom(paramTypes[i])) {
                @SuppressWarnings("unchecked")
                Specification<Object> existing = (Specification<Object>) args[i];
                Specification<Object> spec = DataPermissionHelper.specification(
                        provider, dataPermission.deptField(), dataPermission.userField(), existing);
                args[i] = spec;
                if (log.isDebugEnabled()) {
                    log.debug("Data permission filter applied, scope: {}, method: {}",
                            scope, signature.toShortString());
                }
                break;
            }
        }

        return point.proceed(args);
    }
}
