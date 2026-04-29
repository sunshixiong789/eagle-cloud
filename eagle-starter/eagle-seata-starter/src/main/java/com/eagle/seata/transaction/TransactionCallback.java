package com.eagle.seata.transaction;

/**
 * 全局事务回调函数接口。
 *
 * <p>配合 {@link GlobalTransactionTemplate} 使用，封装需要在 Seata 全局事务中
 * 执行的业务逻辑（有返回值场景）。
 *
 * <p>使用示例：
 * <pre>{@code
 * Long orderId = globalTransactionTemplate.execute("createOrder", () -> {
 *     Long id = orderService.createOrder(request);
 *     inventoryService.deductStock(productId, qty);
 *     return id;
 * });
 * }</pre>
 *
 * @param <T> 业务逻辑返回值类型
 * @author 孙士雄
 * @see GlobalTransactionTemplate
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * 在全局事务上下文中执行业务逻辑。
     *
     * @return 业务逻辑执行结果
     * @throws Exception 业务逻辑执行期间抛出的任意异常，
     *                   由 {@link GlobalTransactionTemplate} 捕获后触发全局回滚
     */
    T doInTransaction() throws Exception;
}
