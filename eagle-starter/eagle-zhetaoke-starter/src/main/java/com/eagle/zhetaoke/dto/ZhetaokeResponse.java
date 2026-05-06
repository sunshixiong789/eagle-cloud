package com.eagle.zhetaoke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 折淘客通用响应包装。
 *
 * @param <T> 内容数据类型
 * @author 孙士雄
 */
@Data
public class ZhetaokeResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** HTTP 状态码，200 表示成功。 */
    private Integer status;

    /** 响应内容数据。 */
    private T content;

    /** 错误信息（失败时返回）。 */
    private String msg;

    /**
     * 判断是否请求成功。
     *
     * @return true 当 status == 200
     */
    public boolean isSuccess() {
        return status != null && status == 200;
    }
}
