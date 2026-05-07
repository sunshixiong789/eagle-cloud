package com.eagle.zhetaoke.jd.request;

import lombok.Data;

/**
 * 京东商品筛选查询请求。
 *
 * @author 孙士雄
 */
@Data
public class JdItemFilterRequest {

    /** 折京客对接秘钥（可选，默认使用配置）。 */
    private String appkey;

    /** 分页页码。 */
    private Integer page;

    /** 每页条数。 */
    private Integer pageSize;

    /** 排序方式。 */
    private String sort;

    /** 一级商品分类。 */
    private Integer cid;
}
