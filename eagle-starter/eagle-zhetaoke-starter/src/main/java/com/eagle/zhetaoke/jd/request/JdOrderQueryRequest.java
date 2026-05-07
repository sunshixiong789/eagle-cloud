package com.eagle.zhetaoke.jd.request;

import lombok.Data;

/**
 * 京东订单查询请求。
 *
 * @author 孙士雄
 */
@Data
public class JdOrderQueryRequest {

    /** 折京客对接秘钥（可选，默认使用配置）。 */
    private String appkey;

    /** 京东联盟 ID。 */
    private String unionId;

    /** 开始时间。 */
    private String startTime;

    /** 结束时间。 */
    private String endTime;

    /** 分页页码。 */
    private Integer page;

    /** 每页条数。 */
    private Integer pageSize;
}
