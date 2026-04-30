package com.eagle.mybatis.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 项目级通用 Mapper 基接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，作为项目中所有 Mapper 的统一基类。
 * 在 {@link BaseMapper} 的基础上提供额外的语义化查询方法，所有继承此接口的 Mapper
 * 将自动获得这些扩展能力。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Mapper
 * public interface UserMapper extends BaseMapperPlus<User> {
 *     // 自定义查询方法
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 * @author eagle
 */
public interface BaseMapperPlus<T> extends BaseMapper<T> {

    /**
     * 根据 ID 列表批量查询实体。
     *
     * <p>委托给 {@link BaseMapper#selectByIds(Collection)}，提供更直观的方法名。
     * 列表为空时直接返回空列表。
     *
     * <p>使用示例：
     * <pre>{@code
     * List<User> users = userMapper.selectBatchByIds(List.of(1L, 2L, 3L));
     * }</pre>
     *
     * @param ids ID 集合；不能为 {@code null}，空集合时返回空列表
     * @return 查询到的实体列表；未找到的 ID 不会出现在结果中
     */
    default List<T> selectBatchByIds(Collection<? extends Serializable> ids) {
        return selectByIds(ids);
    }
}
