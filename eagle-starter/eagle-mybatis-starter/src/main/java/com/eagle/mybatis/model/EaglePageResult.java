package com.eagle.mybatis.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页响应封装。
 *
 * <p>从 MyBatis-Plus 的 {@link IPage} 转换而来，屏蔽框架细节，
 * 提供业务友好的分页元信息（总页数、是否有上/下一页等）。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 从 MP Page 转换
 * IPage<User> mpPage = userMapper.selectPage(new Page<>(1, 20), wrapper);
 * EaglePageResult<User> result = EaglePageResult.of(mpPage);
 *
 * // 转换记录类型（配合 MapStruct 使用）
 * EaglePageResult<UserResponse> response = result.convert(userMapper::toResponse);
 * }</pre>
 *
 * @param <T> 记录类型
 * @author eagle
 */
@Getter
@NoArgsConstructor
public class EaglePageResult<T> {

    /**
     * 当前页数据列表
     */
    private List<T> records;

    /**
     * 符合条件的总记录数
     */
    private long total;

    /**
     * 当前页码（从 1 开始）
     */
    private int pageNum;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 从 MyBatis-Plus {@link IPage} 对象构建分页结果。
     *
     * @param <T>  记录类型
     * @param page MyBatis-Plus 分页对象；不能为 {@code null}
     * @return 封装好的 {@link EaglePageResult}
     */
    public static <T> EaglePageResult<T> of(IPage<T> page) {
        EaglePageResult<T> result = new EaglePageResult<>();
        result.records = page.getRecords() != null ? page.getRecords() : Collections.emptyList();
        result.total = page.getTotal();
        result.pageNum = (int) page.getCurrent();
        result.pageSize = (int) page.getSize();
        result.totalPages = page.getSize() > 0
                ? (int) Math.ceil((double) page.getTotal() / page.getSize())
                : 0;
        result.hasNext = result.pageNum < result.totalPages;
        result.hasPrevious = result.pageNum > 1;
        return result;
    }

    /**
     * 将当前分页结果的记录转换为另一种类型。
     *
     * <p>分页元信息（total、pageNum、pageSize 等）保持不变，仅对 {@link #records} 中的元素应用转换函数。
     * 常用于 DTO 转换：
     * <pre>{@code
     * EaglePageResult<User> users = service.pageQuery(query, wrapper);
     * EaglePageResult<UserResponse> responses = users.convert(userMapper::toResponse);
     * }</pre>
     *
     * @param <R>       目标类型
     * @param converter 转换函数，接收当前类型元素，返回目标类型元素；不能为 {@code null}
     * @return 转换后的新 {@link EaglePageResult}，分页元信息与原对象一致
     */
    public <R> EaglePageResult<R> convert(Function<T, R> converter) {
        EaglePageResult<R> result = new EaglePageResult<>();
        result.records = this.records.stream().map(converter).collect(Collectors.toList());
        result.total = this.total;
        result.pageNum = this.pageNum;
        result.pageSize = this.pageSize;
        result.totalPages = this.totalPages;
        result.hasNext = this.hasNext;
        result.hasPrevious = this.hasPrevious;
        return result;
    }
}
