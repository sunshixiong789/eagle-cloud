package com.eagle.datapermission.context;

import com.eagle.datapermission.enums.DataScope;

import java.util.function.Supplier;

/**
 * 数据权限上下文（线程级覆盖）。
 *
 * <p>基于 {@link ThreadLocal} 提供编程式权限范围覆盖，优先级高于
 * {@link com.eagle.datapermission.provider.DataPermissionProvider} 的返回值。
 * 适用于无法通过注解控制权限的场景（异步任务、定时任务、内部系统调用）。
 *
 * <p><b>使用原则：</b>
 * <ul>
 *   <li>优先使用 {@link #ignorePermission} / {@link #withScope} 的 Lambda 版本，自动清理 ThreadLocal</li>
 *   <li>手动使用 {@link #setScope} 时，必须在 finally 中调用 {@link #clear()}，防止内存泄漏</li>
 *   <li>异步方法（{@code @Async}、线程池）中不继承父线程上下文，需在子线程内重新设置</li>
 * </ul>
 *
 * <pre>{@code
 * // ✅ 推荐：Lambda 版，自动清理
 * List<User> all = DataPermissionContext.ignorePermission(
 *     () -> userRepository.findAll(spec));
 *
 * // ✅ 也可以
 * DataPermissionContext.withScope(DataScope.DEPT, () -> {
 *     exportService.exportByDept(deptId);
 * });
 *
 * // ⚠️ 手动版：必须 finally 清理
 * try {
 *     DataPermissionContext.setScope(DataScope.ALL);
 *     return userRepository.findAll(spec);
 * } finally {
 *     DataPermissionContext.clear();
 * }
 * }</pre>
 *
 * @author 孙士雄
 */
public final class DataPermissionContext {

    private static final ThreadLocal<DataScope> SCOPE_HOLDER = new ThreadLocal<>();

    private DataPermissionContext() {
    }

    /**
     * 获取当前线程显式设置的权限范围。未设置时返回 {@code null}，切面将回退到 Provider 的返回值。
     *
     * @return 当前线程设置的权限范围，或 {@code null}
     */
    public static DataScope getScope() {
        return SCOPE_HOLDER.get();
    }

    /**
     * 在当前线程设置权限范围，覆盖 {@code DataPermissionProvider} 的返回值。
     *
     * <p><b>注意：</b>使用完毕必须在 finally 块中调用 {@link #clear()}，防止内存泄漏。
     *
     * @param scope 权限范围
     */
    public static void setScope(DataScope scope) {
        SCOPE_HOLDER.set(scope);
    }

    /**
     * 清除当前线程的权限范围设置。
     */
    public static void clear() {
        SCOPE_HOLDER.remove();
    }

    /**
     * 在指定权限范围内执行有返回值的操作，执行完毕后自动清除 ThreadLocal。
     *
     * @param scope    权限范围
     * @param supplier 业务操作
     * @param <T>      返回类型
     * @return 业务操作的返回值
     */
    public static <T> T withScope(DataScope scope, Supplier<T> supplier) {
        try {
            SCOPE_HOLDER.set(scope);
            return supplier.get();
        } finally {
            SCOPE_HOLDER.remove();
        }
    }

    /**
     * 在指定权限范围内执行无返回值的操作，执行完毕后自动清除 ThreadLocal。
     *
     * @param scope    权限范围
     * @param runnable 业务操作
     */
    public static void withScope(DataScope scope, Runnable runnable) {
        try {
            SCOPE_HOLDER.set(scope);
            runnable.run();
        } finally {
            SCOPE_HOLDER.remove();
        }
    }

    /**
     * 忽略数据权限，以 {@link DataScope#ALL} 全量数据执行操作（有返回值）。
     *
     * <p>适用于超级管理员操作、定时任务等无需用户维度过滤的场景。
     *
     * @param supplier 业务操作
     * @param <T>      返回类型
     * @return 业务操作的返回值
     */
    public static <T> T ignorePermission(Supplier<T> supplier) {
        return withScope(DataScope.ALL, supplier);
    }

    /**
     * 忽略数据权限，以 {@link DataScope#ALL} 全量数据执行操作（无返回值）。
     *
     * @param runnable 业务操作
     */
    public static void ignorePermission(Runnable runnable) {
        withScope(DataScope.ALL, runnable);
    }
}