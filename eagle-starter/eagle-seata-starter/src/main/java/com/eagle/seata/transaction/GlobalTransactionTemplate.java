package com.eagle.seata.transaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;

/**
 * 编程式 Seata 全局事务模板。
 *
 * <p>提供类似 Spring {@code TransactionTemplate} 的编程式 API，
 * 适用于无法使用 {@code @GlobalTransactional} 注解的场景，例如：
 * <ul>
 *   <li>框架层或通用组件集成（不能依赖 Spring AOP 代理）</li>
 *   <li>循环中每次迭代开启独立全局事务</li>
 *   <li>动态决定是否开启全局事务的业务场景</li>
 *   <li>测试代码中手动控制事务边界</li>
 * </ul>
 *
 * <p>使用示例（有返回值）：
 * <pre>{@code
 * Long orderId = globalTransactionTemplate.execute("createOrder", () -> {
 *     Long id = orderService.createOrder(orderRequest);
 *     inventoryService.deductStock(productId, qty);
 *     return id;
 * });
 * }</pre>
 *
 * <p>使用示例（无返回值）：
 * <pre>{@code
 * globalTransactionTemplate.execute("batchProcess", () ->
 *     items.forEach(item -> itemService.process(item))
 * );
 * }</pre>
 *
 * <p><strong>异常处理</strong>：回调中抛出的任意异常都会触发全局事务回滚，
 * 并以 {@link RuntimeException} 包装后重新抛出。调用方可按需 catch 处理。
 *
 * @author 孙士雄
 * @see TransactionCallback
 * @see GlobalTransactionContext
 */
@Slf4j
public class GlobalTransactionTemplate {

    /**
     * 默认全局事务超时时长（毫秒），60 秒。
     */
    private static final int DEFAULT_TIMEOUT_MS = 60_000;

    /**
     * 在 Seata 全局事务中执行有返回值的业务逻辑。
     *
     * <p>执行流程：
     * <ol>
     *   <li>获取或创建全局事务（支持事务传播：若已有全局事务则加入）</li>
     *   <li>开启全局事务（begin），注册至 Seata TC</li>
     *   <li>执行 {@link TransactionCallback#doInTransaction()} 回调</li>
     *   <li>成功 → commit；失败 → rollback（含回滚失败的日志记录）</li>
     * </ol>
     *
     * @param txName   事务名称，用于 Seata Dashboard 中的链路追踪标识
     * @param callback 需要在全局事务中执行的业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑执行结果
     * @throws RuntimeException 包装原始异常后抛出，原始异常可通过 {@code getCause()} 获取
     */
    public <T> T execute(String txName, TransactionCallback<T> callback) {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        try {
            tx.begin(DEFAULT_TIMEOUT_MS, txName);
            log.debug("[GlobalTransactionTemplate] Transaction '{}' started, xid={}", txName, tx.getXid());

            T result = callback.doInTransaction();

            tx.commit();
            log.debug("[GlobalTransactionTemplate] Transaction '{}' committed, xid={}", txName, tx.getXid());
            return result;
        } catch (Exception ex) {
            log.error("[GlobalTransactionTemplate] Transaction '{}' failed, rolling back. Error: {}",
                    txName, ex.getMessage(), ex);
            try {
                tx.rollback();
                log.debug("[GlobalTransactionTemplate] Transaction '{}' rolled back, xid={}", txName, tx.getXid());
            } catch (TransactionException rollbackEx) {
                // 回滚失败只记录日志，不覆盖原始异常
                log.error("[GlobalTransactionTemplate] Failed to rollback transaction '{}': {}",
                        txName, rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Global transaction '" + txName + "' failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 在 Seata 全局事务中执行无返回值的业务逻辑。
     *
     * <p>内部委托给 {@link #execute(String, TransactionCallback)} 实现，
     * 回调执行完成后返回 {@code null}。
     *
     * @param txName 事务名称，用于 Seata Dashboard 中的链路追踪标识
     * @param action 需要在全局事务中执行的业务逻辑（无返回值）
     * @throws RuntimeException 包装原始异常后抛出
     */
    public void execute(String txName, Runnable action) {
        execute(txName, () -> {
            action.run();
            return null;
        });
    }
}
