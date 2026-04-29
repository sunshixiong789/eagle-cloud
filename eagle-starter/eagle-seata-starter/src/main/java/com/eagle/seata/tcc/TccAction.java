package com.eagle.seata.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

/**
 * TCC（Try-Confirm-Cancel）事务参与者接口模板。
 *
 * <p>TCC 是 Seata 提供的高性能分布式事务模式，适用于对并发性能要求高的场景，
 * 通过资源预留而非数据库行锁实现隔离，相比 AT 模式可显著降低锁竞争。
 *
 * <p>三个阶段语义：
 * <ul>
 *   <li><strong>Try</strong>：预留业务资源（如冻结库存、锁定余额）。
 *       Seata 在一阶段成功后提交全局事务前会调用所有参与者的 Confirm，
 *       任意 Try 失败则调用所有已成功参与者的 Cancel。</li>
 *   <li><strong>Confirm</strong>：确认提交，执行真正的业务操作（如扣减冻结余额）。
 *       必须保证幂等性，Seata 在失败重试时可能多次调用。</li>
 *   <li><strong>Cancel</strong>：回滚，释放 Try 阶段预留的资源（如解冻库存）。
 *       同样必须保证幂等性，并处理空回滚（Try 未执行时 Cancel 被调用）的情况。</li>
 * </ul>
 *
 * <p>实现示例：
 * <pre>{@code
 * @LocalTCC
 * @Service
 * public class InventoryTccService implements TccAction<InventoryTccParam> {
 *
 *     @TwoPhaseBusinessAction(
 *         name = "inventoryTcc",
 *         commitMethod = "confirm",
 *         rollbackMethod = "cancel"
 *     )
 *     @Override
 *     public boolean tryAction(
 *             BusinessActionContext ctx,
 *             @BusinessActionContextParameter("param") InventoryTccParam param) {
 *         // 冻结库存
 *         return inventoryRepository.freezeStock(param.getProductId(), param.getQuantity());
 *     }
 *
 *     @Override
 *     public boolean confirm(BusinessActionContext ctx) {
 *         if (idempotencyHelper.isConfirmed(ctx)) {
 *             return true;
 *         }
 *         // 扣减冻结库存
 *         boolean result = inventoryRepository.deductFrozenStock(
 *             Long.parseLong(ctx.getActionContext("productId").toString()),
 *             Integer.parseInt(ctx.getActionContext("quantity").toString())
 *         );
 *         idempotencyHelper.markConfirmed(ctx);
 *         return result;
 *     }
 *
 *     @Override
 *     public boolean cancel(BusinessActionContext ctx) {
 *         if (idempotencyHelper.isCancelled(ctx)) {
 *             return true;
 *         }
 *         // 释放冻结库存（空回滚场景：若 Try 未执行，直接返回 true）
 *         inventoryRepository.releaseFrozenStock(
 *             Long.parseLong(ctx.getActionContext("productId").toString()),
 *             Integer.parseInt(ctx.getActionContext("quantity").toString())
 *         );
 *         idempotencyHelper.markCancelled(ctx);
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * @param <T> Try 阶段业务参数类型，需被 {@code @BusinessActionContextParameter} 标注以便 Seata 序列化
 * @author 孙士雄
 * @see TccIdempotencyHelper
 */
public interface TccAction<T> {

    /**
     * TCC 第一阶段：预留业务资源。
     *
     * <p>在此阶段完成资源检查与预留（如冻结库存、锁定余额），
     * 不执行实际的业务变更。执行失败（返回 false 或抛出异常）时，
     * Seata 会调用所有已成功参与者的 {@link #cancel} 进行回滚。
     *
     * @param context Seata 业务行为上下文，可通过 {@code context.getActionContext} 获取参数
     * @param param   业务参数，需使用 {@code @BusinessActionContextParameter} 标注参数名以便 Seata 序列化
     * @return {@code true} 表示预留成功，{@code false} 表示预留失败（触发全局回滚）
     */
    boolean tryAction(BusinessActionContext context, T param);

    /**
     * TCC 第二阶段（提交）：确认执行业务操作。
     *
     * <p>在所有参与者 Try 成功后由 Seata 调用，执行真正的业务变更
     * （如扣减冻结余额、核销预留库存）。
     *
     * <p><strong>幂等要求</strong>：Seata 在网络超时等异常情况下会重试，
     * 实现类必须结合 {@link TccIdempotencyHelper} 或数据库唯一约束保证幂等性。
     *
     * @param context Seata 业务行为上下文，包含 xid、branchId 及 Try 阶段传入的参数
     * @return {@code true} 表示 Confirm 成功
     */
    boolean confirm(BusinessActionContext context);

    /**
     * TCC 第二阶段（回滚）：释放预留的业务资源。
     *
     * <p>在任意参与者 Try 失败后由 Seata 调用，回滚 Try 阶段预留的资源
     * （如解冻库存、归还余额）。
     *
     * <p><strong>幂等要求</strong>：同 {@link #confirm}，必须保证幂等性。
     *
     * <p><strong>空回滚处理</strong>：Cancel 可能在 Try 未执行时被调用
     * （Try 所在节点宕机导致 Seata 超时回滚），实现类需识别此场景并直接返回 {@code true}。
     *
     * @param context Seata 业务行为上下文，包含 xid、branchId 及 Try 阶段传入的参数
     * @return {@code true} 表示 Cancel 成功
     */
    boolean cancel(BusinessActionContext context);
}
