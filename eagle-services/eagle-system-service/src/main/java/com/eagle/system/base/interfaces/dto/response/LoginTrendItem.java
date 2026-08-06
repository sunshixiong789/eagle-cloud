package com.eagle.system.base.interfaces.dto.response;

/**
 * 登录趋势数据项
 *
 * @param date  日期（yyyy-MM-dd）
 * @param count 登录次数
 * @author sunshixiong
 */
public record LoginTrendItem(
        String date,
        long count
) {
}
