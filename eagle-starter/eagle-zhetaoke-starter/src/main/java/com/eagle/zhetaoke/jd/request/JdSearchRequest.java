package com.eagle.zhetaoke.jd.request;

import lombok.Data;

/**
 * 京东商品搜索请求。
 *
 * @author 孙士雄
 */
@Data
public class JdSearchRequest {

    /** 折京客对接秘钥（可选，默认使用配置）。 */
    private String appkey;

    /** 分页页码。 */
    private Integer page;

    /** 每页条数。 */
    private Integer pageSize;

    /** 排序方式。 */
    private String sort;

    /** 搜索关键词。 */
    private String q;
}
