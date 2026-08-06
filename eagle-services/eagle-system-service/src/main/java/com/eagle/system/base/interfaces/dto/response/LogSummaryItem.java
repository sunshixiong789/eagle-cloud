package com.eagle.system.base.interfaces.dto.response;

/**
 * 日志类型分布数据项
 *
 * @param logType 日志类型名称
 * @param count   该类型日志数量
 * @author sunshixiong
 */
public record LogSummaryItem(
        String logType,
        long count
) {
}
