package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 基础查询请求参数。
 *
 * <p>封装所有商品查询接口的通用参数：appkey、sid、pid、分页、排序。
 *
 * @author 孙士雄
 */
@Data
public class BaseQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 淘客 PID，格式 mm_xxx_xxx_xxx。 */
    private String pid;

    /** 分页页码，默认 1。 */
    private Integer page = 1;

    /** 每页条数（1-50），默认 20。 */
    private Integer pageSize = 20;

    /** 排序方式，默认 "new"。 */
    private String sort = "new";
}
