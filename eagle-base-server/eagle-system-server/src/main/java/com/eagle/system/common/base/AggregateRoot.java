package com.eagle.system.common.base;

/**
 * 聚合根标记接口
 * <p>
 * 聚合根特征：
 * <ul>
 *   <li>全局唯一标识</li>
 *   <li>一致性边界：保证聚合内部数据的一致性</li>
 *   <li>事务边界：一个事务只能修改一个聚合根</li>
 *   <li>唯一入口：所有对聚合内部的修改必须通过聚合根</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
public interface AggregateRoot {

    /**
     * 获取聚合根的全局唯一标识
     * @return 聚合根的全局唯一标识
     */
    Long getId();
}
