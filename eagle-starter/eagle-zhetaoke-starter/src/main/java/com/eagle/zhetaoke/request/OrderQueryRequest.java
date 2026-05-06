package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单查询请求参数。
 *
 * <p>覆盖京东、美团、饿了么、唯品会、考拉、拼多多、抖音等平台的订单查询。
 *
 * @author 孙士雄
 */
@Data
public class OrderQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 京东联盟 ID（京东专用）。 */
    private String unionId;

    /** 开始时间，格式：yyyy-MM-dd HH:mm:ss。 */
    private String startTime;

    /** 结束时间，格式：yyyy-MM-dd HH:mm:ss。 */
    private String endTime;

    /** 分页页码，默认 1。 */
    private Integer page = 1;

    /** 每页条数，默认 20。 */
    private Integer pageSize = 20;

    /** 平台类型：jd、mt、elm、vip、kaola、pdd、douyin。 */
    private String platform;

    /** 子推客身份标识。 */
    private String positionId;

    /** 推广位 PID。 */
    private String pid;
}
