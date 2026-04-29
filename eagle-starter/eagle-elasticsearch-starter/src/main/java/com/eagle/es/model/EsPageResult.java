package com.eagle.es.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Elasticsearch 分页查询结果封装。
 *
 * <p>统一封装 ES 搜索结果的分页信息，与 Spring Data 的 {@code Page<T>} 解耦，
 * 避免直接暴露框架对象给上层调用方。
 *
 * @param <T> 文档类型
 * @author eagle
 */
@Getter
@RequiredArgsConstructor
public class EsPageResult<T> {

    /** 数据列表 */
    private final List<T> content;

    /** 总数 */
    private final long total;

    /** 当前页（从 1 开始） */
    private final int page;

    /** 每页大小 */
    private final int size;

    /**
     * 计算总页数。
     *
     * @return 总页数；当 size 为 0 时返回 0
     */
    public int getTotalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
