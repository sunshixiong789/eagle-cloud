package com.eagle.datapermission.aspect;

import com.eagle.datapermission.annotation.DataPermission;
import com.eagle.datapermission.annotation.DataPermissionIgnore;
import com.eagle.datapermission.context.DataPermissionContext;
import com.eagle.datapermission.enums.DataScope;
import com.eagle.datapermission.helper.DataPermissionHelper;
import com.eagle.datapermission.properties.DataPermissionProperties;
import com.eagle.datapermission.provider.DataPermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;

/**
 * 数据权限 AOP 切面。
 *
 * <p>拦截 {@link DataPermission} 注解的方法，自动为第一个 {@link Specification} 参数
 * 追加当前用户的数据范围过滤条件。
 *
 * <h2>权限范围判定优先级</h2>
 * <ol>
 *   <li>{@link DataPermissionContext}（ThreadLocal 覆盖，最高优先级）</li>
 *   <li>{@link DataPermissionProvider#getCurrentUserDataScope()}（Security Context）</li>
 * </ol>
 *
 * <h2>跳过数据权限</h2>
 * <ul>
 *   <li>注解方式：在方法上添加 {@link DataPermissionIgnore}</li>
 *   <li>编程式：{@code DataPermissionContext.ignorePermission(() -> ...)}</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
@Order(-2)
public class DataPermissionAspect {

    private final DataPermissionProvider provider;
    private final DataPermissionProperties properties;

    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint point, DataPermission dataPermission) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // @DataPermissionIgnore 优先级高于 @DataPermission，直接放行
        if (method.isAnnotationPresent(DataPermissionIgnore.class)) {
            log.debug("Data permission bypassed by @DataPermissionIgnore, method: {}", signature.toShortString());
            return point.proceed();
        }

        // 权限范围：ThreadLocal 覆盖优先，再读 SecurityContext
        DataScope scope = DataPermissionContext.getScope();
        if (scope == null) {
            scope = provider.getCurrentUserDataScope();
        }
        if (scope == null) {
            log.warn("DataPermissionProvider returned null scope for method [{}], defaulting to SELF",
                    signature.toShortString());
            scope = DataScope.SELF;
        }

        // ALL 范围无需过滤
        if (scope == DataScope.ALL) {
            return point.proceed();
        }

        // 解析字段名：注解空字符串时回退到全局配置
        String deptField = dataPermission.deptField().isEmpty()
                ? properties.getDefaultDeptField()
                : dataPermission.deptField();
        String userField = dataPermission.userField().isEmpty()
                ? properties.getDefaultUserField()
                : dataPermission.userField();

        // 找第一个 Specification 参数注入权限条件
        Object[] args = point.getArgs();
        Class<?>[] paramTypes = signature.getParameterTypes();
        boolean specFound = false;

        for (int i = 0; i < paramTypes.length; i++) {
            if (Specification.class.isAssignableFrom(paramTypes[i])) {
                @SuppressWarnings("unchecked")
                Specification<Object> existing = (Specification<Object>) args[i];
                args[i] = DataPermissionHelper.specification(provider, scope, deptField, userField, existing);
                specFound = true;
                log.debug("Data permission filter applied, scope: {}, deptField: {}, userField: {}, method: {}",
                        scope, deptField, userField, signature.toShortString());
                break;
            }
        }

        // 标注了 @DataPermission 但没有 Specification 参数，权限过滤实际未生效，发出警告
        if (!specFound) {
            log.warn("@DataPermission on method [{}] but no Specification parameter found. " +
                            "Data permission filter was NOT applied. " +
                            "Add a Specification parameter or remove the annotation.",
                    signature.toShortString());
        }

        return point.proceed(args);
    }
}
