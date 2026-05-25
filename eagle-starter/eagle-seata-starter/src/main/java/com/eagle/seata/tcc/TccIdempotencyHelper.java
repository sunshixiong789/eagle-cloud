package com.eagle.seata.tcc;

import lombok.RequiredArgsConstructor;
import org.apache.seata.rm.tcc.api.BusinessActionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCC 阶段幂等辅助工具。
 *
 * <p>在 TCC 的 Confirm/Cancel 阶段，Seata 在失败重试时可能多次调用同一方法。
 * 使用此工具可确保幂等性，避免重复扣款、重复释放库存等问题。
 *
 * <p>使用 {@link BusinessActionContext} 的 {@code xid + branchId} 作为全局唯一键，
 * 记录每个分支事务的执行状态。
 *
 * <p><strong>注意</strong>：默认实现基于内存 {@link ConcurrentHashMap}，
 * 在服务重启后状态丢失，仅适用于开发/测试环境或 JVM 内单次执行场景。
 * 生产环境建议继承此类并覆盖状态存储为 Redis 或数据库，
 * 或直接通过声明自定义 Bean 替换默认实现。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Override
 * public boolean confirm(BusinessActionContext ctx) {
 *     // 已执行，直接返回成功（幂等）
 *     if (idempotencyHelper.isConfirmed(ctx)) {
 *         return true;
 *     }
 *     // 执行业务逻辑
 *     boolean result = doConfirmBusiness(ctx);
 *     // 标记已执行
 *     idempotencyHelper.markConfirmed(ctx);
 *     return result;
 * }
 *
 * @Override
 * public boolean cancel(BusinessActionContext ctx) {
 *     if (idempotencyHelper.isCancelled(ctx)) {
 *         return true;
 *     }
 *     doCancelBusiness(ctx);
 *     idempotencyHelper.markCancelled(ctx);
 *     return true;
 * }
 * }</pre>
 *
 * @author eagle
 * @see TccAction
 */
@RequiredArgsConstructor
public class TccIdempotencyHelper {

    /**
     * TCC 分支事务状态：已确认提交。
     */
    private static final String STATE_CONFIRMED = "CONFIRMED";

    /**
     * TCC 分支事务状态：已取消回滚。
     */
    private static final String STATE_CANCELLED = "CANCELLED";

    /**
     * 内存状态存储：key = xid:branchId，value = 状态常量。
     *
     * <p>使用 {@link ConcurrentHashMap} 保证线程安全。
     * 生产环境替换为持久化存储（Redis/DB）时，覆盖此字段或重写相关方法。
     */
    private final Map<String, String> tccState = new ConcurrentHashMap<>();

    /**
     * 判断该分支事务的 Confirm 阶段是否已执行（幂等检查）。
     *
     * @param ctx Seata 业务行为上下文，提供 xid 和 branchId
     * @return {@code true} 表示 Confirm 已执行，可安全跳过
     */
    public boolean isConfirmed(BusinessActionContext ctx) {
        return STATE_CONFIRMED.equals(tccState.get(buildKey(ctx)));
    }

    /**
     * 判断该分支事务的 Cancel 阶段是否已执行（幂等检查）。
     *
     * @param ctx Seata 业务行为上下文，提供 xid 和 branchId
     * @return {@code true} 表示 Cancel 已执行，可安全跳过
     */
    public boolean isCancelled(BusinessActionContext ctx) {
        return STATE_CANCELLED.equals(tccState.get(buildKey(ctx)));
    }

    /**
     * 标记该分支事务的 Confirm 阶段已执行。
     *
     * <p>应在 Confirm 业务逻辑成功执行后调用，建议与业务操作在同一数据库事务中完成
     * （生产环境持久化存储时）。
     *
     * @param ctx Seata 业务行为上下文，提供 xid 和 branchId
     */
    public void markConfirmed(BusinessActionContext ctx) {
        tccState.put(buildKey(ctx), STATE_CONFIRMED);
    }

    /**
     * 标记该分支事务的 Cancel 阶段已执行。
     *
     * <p>应在 Cancel 业务逻辑成功执行后调用。
     *
     * @param ctx Seata 业务行为上下文，提供 xid 和 branchId
     */
    public void markCancelled(BusinessActionContext ctx) {
        tccState.put(buildKey(ctx), STATE_CANCELLED);
    }

    /**
     * 构建分支事务的唯一键。
     *
     * <p>格式：{@code xid:branchId}，全局唯一标识一次分支事务执行。
     *
     * @param ctx Seata 业务行为上下文
     * @return 唯一键字符串
     */
    private String buildKey(BusinessActionContext ctx) {
        return ctx.getXid() + ":" + ctx.getBranchId();
    }
}
