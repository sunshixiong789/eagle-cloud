package com.eagle.system.base.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录趋势数据项
 *
 * @author sunshixiong
 */
@Data
@AllArgsConstructor
public class LoginTrendItem {

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 登录次数
     */
    private long count;
}
