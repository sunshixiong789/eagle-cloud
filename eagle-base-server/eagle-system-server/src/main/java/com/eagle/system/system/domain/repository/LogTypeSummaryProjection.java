package com.eagle.system.system.domain.repository;

import com.eagle.system.domain.model.enums.LogType;

/**
 * JPQL GROUP BY logType 投影
 *
 * @author sunshixiong
 */
public interface LogTypeSummaryProjection {

    /** 日志类型 */
    LogType getLogType();

    /** 该类型日志数量 */
    Long getCount();
}
