package com.eagle.system.system.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日志类型分布数据项
 *
 * @author sunshixiong
 */
@Data
@AllArgsConstructor
public class LogSummaryItem {

    /** 日志类型名称 */
    private String logType;

    /** 该类型日志数量 */
    private long count;
}
