package com.eagle.seata.util;

import org.apache.seata.core.context.RootContext;
import org.springframework.util.StringUtils;

/**
 * Seata 分布式事务工具类。
 *
 * <p>封装 {@link RootContext} 常用操作，提供全局事务 XID 的获取、绑定、解绑能力。
 * 本类为无状态工具类，所有方法均为静态方法，线程安全。
 *
 * <p>使用示例：
 * <pre>
 * // 判断是否在全局事务中
 * if (SeataUtil.inGlobalTransaction()) {
 *     String xid = SeataUtil.getXid();
 *     log.info("Current XID: {}", xid);
 * }
 * </pre>
 *
 * @author 孙士雄
 */
public final class SeataUtil {

    /**
     * 私有构造函数，防止实例化。
     */
    private SeataUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前线程绑定的全局事务 XID。
     *
     * @return 当前 XID，若未在全局事务中则返回 {@code null}
     */
    public static String getXid() {
        return RootContext.getXID();
    }

    /**
     * 判断当前线程是否处于全局事务中。
     *
     * @return {@code true} 表示已绑定 XID（处于全局事务中）
     */
    public static boolean inGlobalTransaction() {
        return StringUtils.hasText(RootContext.getXID());
    }

    /**
     * 手动将指定 XID 绑定到当前线程。
     *
     * <p>通常用于跨线程传播 XID 的场景（如异步任务、线程池任务）。
     * 绑定后务必在 finally 块中调用 {@link #unbind()} 释放，防止内存泄漏。
     *
     * @param xid 全局事务 XID，不得为空
     */
    public static void bind(String xid) {
        RootContext.bind(xid);
    }

    /**
     * 解绑当前线程的全局事务 XID。
     *
     * <p>在手动 {@link #bind(String)} 之后，必须在 finally 块中调用此方法，
     * 防止 XID 残留导致事务污染或内存泄漏。
     */
    public static void unbind() {
        RootContext.unbind();
    }
}
