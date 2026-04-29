package com.eagle.mybatis.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * MyBatis-Plus 查询条件辅助工具类。
 *
 * <p>封装常用的 {@link LambdaQueryWrapper} 构建片段，减少业务层样板判空代码，
 * 提升查询条件拼装的可读性。
 *
 * <p>使用示例：
 * <pre>{@code
 * LambdaQueryWrapper<User> wrapper = QueryHelper.likeAny("张三",
 *     User::getName, User::getPhone);
 * QueryHelper.dateBetween(wrapper, User::getCreateTime,
 *     request.getStartTime(), request.getEndTime());
 * QueryHelper.conditionEq(wrapper, request.getStatus() != null,
 *     User::getStatus, request.getStatus());
 * }</pre>
 *
 * <p>此类为无状态工具类，所有方法均为静态方法，禁止实例化。
 *
 * @author eagle
 */
public final class QueryHelper {

    /**
     * 禁止实例化。
     */
    private QueryHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 对多个字段构建关键词模糊查询（多字段 OR LIKE）。
     *
     * <p>仅当 {@code keyword} 非空时才添加查询条件；各字段之间使用 OR 关系，生成：
     * <pre>{@code AND (field1 LIKE '%keyword%' OR field2 LIKE '%keyword%')}</pre>
     *
     * <p>适用于全局搜索框跨多列模糊查询的场景。
     *
     * <p>使用示例：
     * <pre>{@code
     * LambdaQueryWrapper<User> wrapper = QueryHelper.likeAny("张三",
     *     User::getName, User::getPhone, User::getEmail);
     * }</pre>
     *
     * @param <T>     实体类型
     * @param keyword 搜索关键词；为空时返回空的 {@link LambdaQueryWrapper}（无过滤条件）
     * @param columns 需要模糊匹配的字段 lambda 引用列表；不能为 {@code null}
     * @return 配置好的 {@link LambdaQueryWrapper}；keyword 为空时返回无条件 wrapper
     */
    @SafeVarargs
    public static <T> LambdaQueryWrapper<T> likeAny(String keyword, SFunction<T, ?>... columns) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.hasText(keyword) || columns == null || columns.length == 0) {
            return wrapper;
        }
        wrapper.and(w -> {
            for (int i = 0; i < columns.length; i++) {
                if (i == 0) {
                    w.like(columns[i], keyword);
                } else {
                    w.or().like(columns[i], keyword);
                }
            }
        });
        return wrapper;
    }

    /**
     * 在现有 {@link LambdaQueryWrapper} 上追加单字段模糊查询（LIKE）。
     *
     * <p>仅当 {@code value} 有文本时追加条件，为空时跳过。
     *
     * @param <T>     实体类型
     * @param wrapper 已有的查询条件包装器；不能为 {@code null}
     * @param column  字段 lambda 引用；不能为 {@code null}
     * @param value   模糊查询关键词；为空时跳过
     */
    public static <T> void likeIfPresent(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, ?> column,
                                          String value) {
        if (StringUtils.hasText(value)) {
            wrapper.like(column, value);
        }
    }

    /**
     * 在现有 {@link LambdaQueryWrapper} 上追加日期范围查询条件（含左右边界）。
     *
     * <p>仅当 {@code start} 或 {@code end} 非 {@code null} 时才追加对应的边界条件，
     * 支持只设置起始时间或只设置结束时间（半开区间）。
     *
     * <p>使用示例：
     * <pre>{@code
     * LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
     * QueryHelper.dateBetween(wrapper, Order::getCreateTime,
     *     request.getStartTime(), request.getEndTime());
     * }</pre>
     *
     * @param <T>     实体类型
     * @param wrapper 已有的查询条件包装器；不能为 {@code null}
     * @param column  日期字段 lambda 引用；不能为 {@code null}
     * @param start   范围起始时间（gte），{@code null} 表示不限下界
     * @param end     范围结束时间（lte），{@code null} 表示不限上界
     * @return 传入的 {@code wrapper} 本身（支持链式调用）
     */
    public static <T> LambdaQueryWrapper<T> dateBetween(LambdaQueryWrapper<T> wrapper,
                                                          SFunction<T, ?> column,
                                                          LocalDateTime start,
                                                          LocalDateTime end) {
        if (start != null) {
            wrapper.ge(column, start);
        }
        if (end != null) {
            wrapper.le(column, end);
        }
        return wrapper;
    }

    /**
     * 有条件地在 {@link LambdaQueryWrapper} 上追加精确匹配（eq）条件。
     *
     * <p>相当于 MyBatis-Plus 原生的 {@code wrapper.eq(condition, column, value)}，
     * 但以独立方法形式提供，返回 {@code wrapper} 本身以便链式追加其他条件：
     *
     * <p>使用示例：
     * <pre>{@code
     * LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
     * QueryHelper.conditionEq(wrapper, request.getStatus() != null,
     *     User::getStatus, request.getStatus());
     * QueryHelper.conditionEq(wrapper, request.getDeptId() != null,
     *     User::getDeptId, request.getDeptId());
     * }</pre>
     *
     * @param <T>       实体类型
     * @param wrapper   已有的查询条件包装器；不能为 {@code null}
     * @param condition 条件为 {@code true} 时才追加 eq 子句
     * @param column    字段 lambda 引用；不能为 {@code null}
     * @param value     精确匹配值
     * @return 传入的 {@code wrapper} 本身（支持继续追加条件）
     */
    public static <T> LambdaQueryWrapper<T> conditionEq(LambdaQueryWrapper<T> wrapper,
                                                          boolean condition,
                                                          SFunction<T, ?> column,
                                                          Object value) {
        wrapper.eq(condition, column, value);
        return wrapper;
    }

    /**
     * 仅当 {@code value} 非 {@code null} 时，在 wrapper 上追加精确匹配（eq）条件。
     *
     * <p>此方法为 {@link #conditionEq(LambdaQueryWrapper, boolean, SFunction, Object)} 的便捷重载，
     * 自动以 {@code value != null} 作为条件判断。
     *
     * @param <T>     实体类型
     * @param wrapper 已有的查询条件包装器；不能为 {@code null}
     * @param column  字段 lambda 引用；不能为 {@code null}
     * @param value   等值条件（{@code null} 则跳过）
     */
    public static <T> void conditionEqIfPresent(LambdaQueryWrapper<T> wrapper,
                                                 SFunction<T, ?> column,
                                                 Object value) {
        wrapper.eq(value != null, column, value);
    }

    /**
     * 仅当 {@code values} 非空时，在 wrapper 上追加 IN 条件。
     *
     * @param <T>     实体类型
     * @param wrapper 已有的查询条件包装器；不能为 {@code null}
     * @param column  字段 lambda 引用；不能为 {@code null}
     * @param values  候选值集合；{@code null} 或空集合时跳过
     */
    public static <T> void conditionIn(LambdaQueryWrapper<T> wrapper,
                                        SFunction<T, ?> column,
                                        Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            wrapper.in(column, values);
        }
    }
}
