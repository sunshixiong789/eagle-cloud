package com.eagle.system.system.domain.repository;

import java.time.LocalDate;

/** JPQL GROUP BY date 投影，用于登录趋势查询 */
public interface LoginTrendProjection {
    LocalDate getDate();
    Long getCount();
}
